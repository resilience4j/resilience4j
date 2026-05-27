# Hedge Lifecycle & Daemon Scheduling Fix

**Issue:** #2451 — Virtual thread scheduler optimization - Hedge lifecycle + Core one-shot primitive

**Scope:** Phase 1 only — lifecycle correctness and daemon policy. No new scheduler abstractions.

---

## Problem Statement

`HedgeImpl` creates a `ContextAwareScheduledThreadPoolExecutor` per instance (line 62-67) but exposes no way to shut it down. The internal scheduler is never closed, causing:

1. **Thread leak** — each `Hedge` instance owns `concurrentHedges` (default 10) threads that outlive the instance
2. **JVM shutdown hang** — `NamingThreadFactory` forces `setDaemon(false)` (line 49), so internal scheduler threads prevent JVM exit
3. **Virtual thread incompatibility** — `setDaemon(false)` throws `IllegalThreadStateException` on virtual threads

This breaks the pattern established by `ThreadPoolBulkhead`, which implements `AutoCloseable` with proper shutdown cascading through its registry.

## Design Decisions

### Decision 1: daemon=true for library-owned threads (scoped)

Add a `daemon` flag to `ContextAwareScheduledThreadPoolExecutor.Builder`. Hedge passes `daemon=true`.

**Why scoped, not global:** `NamingThreadFactory` is shared with `BulkheadNamingThreadFactory`. Changing `NamingThreadFactory` globally to daemon=true would alter Bulkhead thread behavior beyond this PR's scope. The builder flag isolates the change to Hedge only.

**Why daemon at all:** CircuitBreaker and RateLimiter both create daemon threads for their internal schedulers. Library-managed helper threads should not prevent JVM shutdown. daemon=true solves the JVM lifecycle issue; AutoCloseable solves deterministic cleanup.

### Decision 2: AutoCloseable lifecycle (mirror ThreadPoolBulkhead)

`Hedge extends AutoCloseable` and `HedgeRegistry extends AutoCloseable`, following the exact pattern from `ThreadPoolBulkhead` / `ThreadPoolBulkheadRegistry`.

**Responsibilities separated:**
- `daemon=true` = JVM shutdown safety
- `close()` = deterministic resource cleanup and test hygiene

### Decision 3: No cancellation semantics changes

The `cancel(false)` / `cancel(true)` patterns in `decorateCaller` (lines 118-119, 127) are intentionally unchanged. This PR only adds a shutdown boundary; it does not alter in-flight cancellation behavior.

## API Changes

### Hedge interface

```java
public interface Hedge extends AutoCloseable {
    /**
     * Closes the Hedge and shuts down its internal scheduled executor.
     * Idempotent — multiple invocations have no additional effect.
     * After close(), submit() and decorateCompletionStage() will throw
     * RejectedExecutionException from the underlying executor.
     * In-flight work is allowed to complete (graceful drain).
     */
    @Override
    void close();

    // ... all existing methods unchanged ...
}
```

### HedgeRegistry interface

```java
public interface HedgeRegistry extends Registry<Hedge, HedgeConfig>, AutoCloseable {
    @Override
    void close();
}
```

### NamingThreadFactory — new constructor

```java
public NamingThreadFactory(String name, boolean daemon) {
    this.group = getThreadGroup();
    this.prefix = String.join("-", name, "");
    this.daemon = daemon;
}
```

Existing `NamingThreadFactory(String name)` constructor preserved with `daemon=false` default — binary compatible, BulkheadNamingThreadFactory unaffected.

### ContextAwareScheduledThreadPoolExecutor.Builder — daemon flag

```java
public Builder daemon(boolean daemon) {
    this.daemon = daemon;
    return this;
}
```

Default `daemon=false` preserves backward compatibility for all existing callers.

### HedgeImpl — constructor change + close()

Constructor adds `.daemon(true)`:
```java
this.configuredHedgeExecutor =
    ContextAwareScheduledThreadPoolExecutor
        .newScheduledThreadPool()
        .corePoolSize(hedgeConfig.getConcurrentHedges())
        .contextPropagators(hedgeConfig.getContextPropagators())
        .daemon(true)
        .build();
```

New `close()` method (mirrors `FixedThreadPoolBulkhead.close()`):
```java
@Override
public void close() {
    configuredHedgeExecutor.shutdown();
    try {
        if (!configuredHedgeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            configuredHedgeExecutor.shutdownNow();
        }
    } catch (InterruptedException e) {
        if (!configuredHedgeExecutor.isTerminated()) {
            configuredHedgeExecutor.shutdownNow();
        }
        Thread.currentThread().interrupt();
    }
}
```

`shutdown()` is inherently idempotent. `awaitTermination()` is safe to call repeatedly on a terminal executor. `shutdownNow()` returns discarded tasks — this is intentional since queued hedge timers are no longer meaningful for a disposing executor.

### InMemoryHedgeRegistry — cascade close

```java
@Override
public void close() {
    for (Hedge hedge : getAllHedges().collect(java.util.stream.Collectors.toList())) {
        hedge.close();
    }
}
```

`Collectors.toList()` creates a defensive copy, avoiding ConcurrentModificationException if close() triggers deregistration.

## Cancellation Semantics (Unchanged)

For reference, these paths are NOT modified:

**Primary wins:**
- `sf.cancel(true)` — interrupts scheduled hedge timer
- `hedged.cancel(false)` — cancels secondary without thread interrupt

**Hedge wins:**
- `supplied.cancel(false)` — cancels primary without thread interrupt

`cancel(false)` means underlying threads are never interrupted by the library — user executor threads always complete naturally. Only the library's own timer thread may be interrupted via `cancel(true)`.

## Migration Safety

- **Binary compatible:** `NamingThreadFactory(String)` unchanged. `ContextAwareScheduledThreadPoolExecutor.Builder` default `daemon=false` unchanged.
- **Source compatible:** `Hedge` gains `close()` — only `HedgeImpl` implements `Hedge`, verified no external implementations exist (no service loader, no module-info, no `implements Hedge` outside `HedgeImpl`).
- **Behavioral:** Existing code that doesn't call `close()` sees no change — daemon threads simply don't prevent JVM exit anymore.

## Test Coverage

| Test | Guarantee |
|------|-----------|
| submit-after-close throws RejectedExecutionException | lifecycle boundary |
| in-flight work completes on close | graceful drain |
| close() idempotent | cleanup safety |
| scheduled-but-not-started tasks on close | executor contract adherence |
| registry cascade close | ownership propagation |
| NamingThreadFactory daemon constructor | daemon flag correctness |
