# Hedge Lifecycle & Daemon Scheduling Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add AutoCloseable lifecycle and daemon thread policy to Hedge, fixing scheduler thread leaks and virtual thread incompatibility.

**Architecture:** Mirror ThreadPoolBulkhead's AutoCloseable + registry cascade pattern. Add scoped daemon flag to ContextAwareScheduledThreadPoolExecutor.Builder. No new abstractions, no cancellation changes.

**Tech Stack:** Java 8+, Gradle, JUnit 4, AssertJ, Mockito

**Spec:** `docs/superpowers/specs/2026-05-27-hedge-lifecycle-design.md`

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `resilience4j-core/.../NamingThreadFactory.java` | Modify | Add daemon constructor overload |
| `resilience4j-core/.../ContextAwareScheduledThreadPoolExecutor.java` | Modify | Add daemon builder flag |
| `resilience4j-hedge/.../Hedge.java` | Modify | Extend AutoCloseable |
| `resilience4j-hedge/.../HedgeImpl.java` | Modify | Add daemon=true, implement close() |
| `resilience4j-hedge/.../HedgeRegistry.java` | Modify | Extend AutoCloseable |
| `resilience4j-hedge/.../InMemoryHedgeRegistry.java` | Modify | Implement cascade close() |
| `resilience4j-core/.../NamingThreadFactoryTest.java` | Create | Test daemon constructor |
| `resilience4j-hedge/.../HedgeImplTest.java` | Modify | Add close behavior tests |
| `resilience4j-hedge/.../InMemoryHedgeRegistryTest.java` | Modify | Add cascade close test |

---

### Task 1: Add daemon constructor to NamingThreadFactory

**Files:**
- Modify: `resilience4j-core/src/main/java/io/github/resilience4j/core/NamingThreadFactory.java`

- [ ] **Step 1: Write the failing test**

Create `resilience4j-core/src/test/java/io/github/resilience4j/core/NamingThreadFactoryTest.java`:

```java
package io.github.resilience4j.core;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class NamingThreadFactoryTest {

    @Test
    public void shouldCreateDaemonThreadWhenDaemonTrue() {
        NamingThreadFactory factory = new NamingThreadFactory("test", true);
        AtomicBoolean wasDaemon = new AtomicBoolean(false);

        Thread thread = factory.newThread(() -> {
            wasDaemon.set(Thread.currentThread().isDaemon());
        });

        assertThat(thread.isDaemon()).isTrue();
        assertThat(thread.getName()).startsWith("test-");
    }

    @Test
    public void shouldCreateNonDaemonThreadWhenDaemonFalse() {
        NamingThreadFactory factory = new NamingThreadFactory("test", false);
        Thread thread = factory.newThread(() -> {});

        assertThat(thread.isDaemon()).isFalse();
    }

    @Test
    public void shouldDefaultToNonDaemon() {
        NamingThreadFactory factory = new NamingThreadFactory("test");
        Thread thread = factory.newThread(() -> {});

        assertThat(thread.isDaemon()).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :resilience4j-core:test --tests "io.github.resilience4j.core.NamingThreadFactoryTest" -i`
Expected: FAIL — constructor `NamingThreadFactory(String, boolean)` does not exist

- [ ] **Step 3: Implement daemon constructor**

Replace the entirety of `resilience4j-core/src/main/java/io/github/resilience4j/core/NamingThreadFactory.java`:

```java
package io.github.resilience4j.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates threads using "$name-%d" pattern for naming. Is based on {@link Executors#defaultThreadFactory}
 */
public class NamingThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String prefix;
    private final boolean daemon;

    public NamingThreadFactory(String name) {
        this(name, false);
    }

    public NamingThreadFactory(String name, boolean daemon) {
        this.group = getThreadGroup();
        this.prefix = String.join("-", name, "");
        this.daemon = daemon;
    }

    private ThreadGroup getThreadGroup() {
        SecurityManager security = System.getSecurityManager();
        return security != null ? security.getThreadGroup()
            : Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(group, runnable, createName(), 0);
        thread.setDaemon(daemon);
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }

    private String createName() {
        return prefix + threadNumber.getAndIncrement();
    }
}
```

Key change: `setDaemon(false)` replaced with `setDaemon(daemon)` controlled by constructor parameter. Original constructor defaults to `false`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :resilience4j-core:test --tests "io.github.resilience4j.core.NamingThreadFactoryTest" -i`
Expected: PASS

- [ ] **Step 5: Run existing core tests to verify no regression**

Run: `./gradlew :resilience4j-core:test -i`
Expected: ALL PASS — existing `ContextAwareScheduledThreadPoolExecutorTest` uses `NamingThreadFactory(String)` which still defaults to daemon=false

- [ ] **Step 6: Commit**

```bash
git add resilience4j-core/src/main/java/io/github/resilience4j/core/NamingThreadFactory.java resilience4j-core/src/test/java/io/github/resilience4j/core/NamingThreadFactoryTest.java
git commit -m "feat(core): add daemon constructor to NamingThreadFactory

Adds NamingThreadFactory(String, boolean) overload for daemon thread
creation. Existing constructor defaults to daemon=false for backward
compatibility. Needed by Hedge lifecycle fix (#2451)."
```

---

### Task 2: Add daemon flag to ContextAwareScheduledThreadPoolExecutor.Builder

**Files:**
- Modify: `resilience4j-core/src/main/java/io/github/resilience4j/core/ContextAwareScheduledThreadPoolExecutor.java`

- [ ] **Step 1: Write the failing test**

Add to a new test file `resilience4j-core/src/test/java/io/github/resilience4j/core/ContextAwareScheduledThreadPoolExecutorDaemonTest.java`:

```java
package io.github.resilience4j.core;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextAwareScheduledThreadPoolExecutorDaemonTest {

    @Test
    public void shouldCreateDaemonThreadsWhenDaemonTrue() throws Exception {
        ContextAwareScheduledThreadPoolExecutor executor =
            ContextAwareScheduledThreadPoolExecutor.newScheduledThreadPool()
                .corePoolSize(1)
                .daemon(true)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean wasDaemon = new AtomicBoolean(false);

        executor.schedule(() -> {
            wasDaemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        }, 0, TimeUnit.MILLISECONDS);

        latch.await(5, TimeUnit.SECONDS);
        assertThat(wasDaemon.get()).isTrue();
        executor.shutdown();
    }

    @Test
    public void shouldCreateNonDaemonThreadsByDefault() throws Exception {
        ContextAwareScheduledThreadPoolExecutor executor =
            ContextAwareScheduledThreadPoolExecutor.newScheduledThreadPool()
                .corePoolSize(1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean wasDaemon = new AtomicBoolean(false);

        executor.schedule(() -> {
            wasDaemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        }, 0, TimeUnit.MILLISECONDS);

        latch.await(5, TimeUnit.SECONDS);
        assertThat(wasDaemon.get()).isFalse();
        executor.shutdown();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :resilience4j-core:test --tests "io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutorDaemonTest" -i`
Expected: FAIL — `daemon(boolean)` method does not exist on Builder

- [ ] **Step 3: Implement daemon flag on Builder**

Modify `resilience4j-core/src/main/java/io/github/resilience4j/core/ContextAwareScheduledThreadPoolExecutor.java`:

Change the constructor to accept `daemon` parameter:

```java
private ContextAwareScheduledThreadPoolExecutor(int corePoolSize,
                                               @Nullable List<ContextPropagator> contextPropagators,
                                               boolean daemon) {
    super(corePoolSize, new NamingThreadFactory(THREAD_PREFIX, daemon));
    this.contextPropagators = contextPropagators != null ? contextPropagators : new ArrayList<>();
}
```

Change the Builder to add daemon field and method:

```java
public static class Builder {
    private List<ContextPropagator> contextPropagators = new ArrayList<>();
    private int corePoolSize;
    private boolean daemon = false;

    public Builder corePoolSize(int corePoolSize) {
        if (corePoolSize < 1) {
            throw new IllegalArgumentException(
                "corePoolSize must be a positive integer value >= 1");
        }
        this.corePoolSize = corePoolSize;
        return this;
    }

    public Builder contextPropagators(ContextPropagator... contextPropagators) {
        this.contextPropagators = contextPropagators != null ?
            Arrays.stream(contextPropagators).collect(toList()) :
            new ArrayList<>();
        return this;
    }

    public Builder daemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public ContextAwareScheduledThreadPoolExecutor build() {
        return new ContextAwareScheduledThreadPoolExecutor(corePoolSize, contextPropagators, daemon);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :resilience4j-core:test --tests "io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutorDaemonTest" -i`
Expected: PASS

- [ ] **Step 5: Run all core tests**

Run: `./gradlew :resilience4j-core:test -i`
Expected: ALL PASS — existing tests don't use `daemon()`, default is false

- [ ] **Step 6: Commit**

```bash
git add resilience4j-core/src/main/java/io/github/resilience4j/core/ContextAwareScheduledThreadPoolExecutor.java resilience4j-core/src/test/java/io/github/resilience4j/core/ContextAwareScheduledThreadPoolExecutorDaemonTest.java
git commit -m "feat(core): add daemon flag to ContextAwareScheduledThreadPoolExecutor

Builder.daemon(true) creates daemon threads via NamingThreadFactory.
Default is false for backward compatibility. Part of #2451."
```

---

### Task 3: Make Hedge extend AutoCloseable

**Files:**
- Modify: `resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/Hedge.java`

- [ ] **Step 1: Modify Hedge interface**

In `resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/Hedge.java`, change line 42 from:

```java
public interface Hedge {
```

to:

```java
public interface Hedge extends AutoCloseable {
```

Add the close() method declaration after the `onSecondaryFailure` method (after line 232), before the `EventPublisher` interface:

```java
    /**
     * Closes the Hedge and shuts down its internal scheduled executor.
     * <p>
     * This method is idempotent. Multiple invocations have no additional effect.
     * After close(), {@link #submit} and {@link #decorateCompletionStage} will
     * throw {@link java.util.concurrent.RejectedExecutionException} from the
     * underlying executor.
     * <p>
     * In-flight work is allowed to complete (graceful drain). The method waits
     * up to 5 seconds for running tasks to finish, then forces shutdown.
     */
    @Override
    void close();
```

- [ ] **Step 2: Verify compilation fails on HedgeImpl**

Run: `./gradlew :resilience4j-hedge:compileJava 2>&1 | tail -5`
Expected: FAIL — `HedgeImpl` does not implement `close()`

- [ ] **Step 3: Commit interface change**

```bash
git add resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/Hedge.java
git commit -m "feat(hedge): make Hedge extend AutoCloseable

Adds close() contract to Hedge interface. Implementation follows
in HedgeImpl. Part of #2451."
```

---

### Task 4: Implement close() in HedgeImpl with daemon scheduling

**Files:**
- Modify: `resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/internal/HedgeImpl.java`

- [ ] **Step 1: Write the failing tests**

Add to `resilience4j-hedge/src/test/java/io/github/resilience4j/hedge/internal/HedgeImplTest.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// ... add these tests at the end of the class ...

    @Test
    public void shouldRejectSubmitAfterClose() {
        Hedge hedge = Hedge.ofDefaults("closeTest");
        hedge.close();

        assertThatThrownBy(() -> hedge.submit(() -> "value", Executors.newSingleThreadExecutor()))
            .isInstanceOf(RejectedExecutionException.class);
    }

    @Test
    public void shouldDrainInFlightWorkOnClose() throws Exception {
        Hedge hedge = Hedge.ofDefaults("drainTest");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> future = hedge.submit(() -> {
            started.countDown();
            finish.await(10, TimeUnit.SECONDS);
            completed.set(true);
            return "done";
        }, executor);

        started.await(5, TimeUnit.SECONDS);
        hedge.close();
        finish.countDown();

        future.handle((result, ex) -> {
            assertThat(result).isEqualTo("done");
            return null;
        });
        executor.shutdown();
    }

    @Test
    public void shouldBeIdempotentOnClose() {
        Hedge hedge = Hedge.ofDefaults("idempotentTest");

        assertThatCode(() -> {
            hedge.close();
            hedge.close();
            hedge.close();
        }).doesNotThrowAnyException();
    }

    @Test
    public void shouldHandleScheduledButNotStartedTaskOnClose() throws Exception {
        HedgeConfig config = HedgeConfig.custom()
            .preconfiguredDuration(Duration.ofSeconds(30))
            .build();
        Hedge hedge = Hedge.of("scheduledCloseTest", config);

        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>("none");

        ExecutorService executor = Executors.newCachedThreadPool();
        CompletableFuture<String> future = hedge.submit(() -> {
            started.countDown();
            return "primary";
        }, executor);

        // Close before hedge delay fires (30s delay, we close immediately)
        Thread.sleep(100);
        hedge.close();

        // Primary should still complete — it was already submitted
        future.handle((r, ex) -> {
            result.set(r != null ? r : ex.getClass().getSimpleName());
            return null;
        });

        assertThat(future.isDone()).isTrue();
        executor.shutdown();
    }
```

Note: Add `import static org.assertj.core.api.Assertions.assertThatThrownBy;` and `import static org.assertj.core.api.Assertions.assertThatCode;` if not already present.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :resilience4j-hedge:test --tests "io.github.resilience4j.hedge.internal.HedgeImplTest" -i`
Expected: FAIL — `close()` not implemented

- [ ] **Step 3: Implement HedgeImpl changes**

In `resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/internal/HedgeImpl.java`:

**Change 1:** Add `.daemon(true)` to constructor (line 62-67):

```java
        this.configuredHedgeExecutor =
            ContextAwareScheduledThreadPoolExecutor
                .newScheduledThreadPool()
                .corePoolSize(hedgeConfig.getConcurrentHedges())
                .contextPropagators(hedgeConfig.getContextPropagators())
                .daemon(true)
                .build();
```

**Change 2:** Add `close()` method after the `publishEvent` method (after line 245):

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

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :resilience4j-hedge:test --tests "io.github.resilience4j.hedge.internal.HedgeImplTest" -i`
Expected: ALL PASS

- [ ] **Step 5: Run all hedge tests**

Run: `./gradlew :resilience4j-hedge:test -i`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/internal/HedgeImpl.java resilience4j-hedge/src/test/java/io/github/resilience4j/hedge/internal/HedgeImplTest.java
git commit -m "feat(hedge): implement close() with daemon scheduling

HedgeImpl.close() shuts down internal scheduler with graceful drain
(up to 5s) then forced shutdown. Constructor now uses daemon=true
threads via ContextAwareScheduledThreadPoolExecutor.Builder.

Fixes scheduler thread leak and virtual thread incompatibility.
Part of #2451."
```

---

### Task 5: Make HedgeRegistry extend AutoCloseable with cascade close

**Files:**
- Modify: `resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/HedgeRegistry.java`
- Modify: `resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/internal/InMemoryHedgeRegistry.java`

- [ ] **Step 1: Write the failing test**

Add to `resilience4j-hedge/src/test/java/io/github/resilience4j/hedge/internal/InMemoryHedgeRegistryTest.java`:

```java
import java.util.stream.Collectors;

// ... add this import and test ...

    @Test
    public void shouldCascadeCloseToAllHedges() throws Exception {
        HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();

        Hedge hedge1 = registry.hedge("hedge1");
        Hedge hedge2 = registry.hedge("hedge2");
        Hedge hedge3 = registry.hedge("hedge3");

        registry.close();

        // After registry close, all hedges should reject new submissions
        assertThatThrownBy(() -> hedge1.submit(() -> "val", java.util.concurrent.Executors.newSingleThreadExecutor()))
            .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
        assertThatThrownBy(() -> hedge2.submit(() -> "val", java.util.concurrent.Executors.newSingleThreadExecutor()))
            .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
        assertThatThrownBy(() -> hedge3.submit(() -> "val", java.util.concurrent.Executors.newSingleThreadExecutor()))
            .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
    }
```

Add `import static org.assertj.core.api.Assertions.assertThatThrownBy;` if not present.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :resilience4j-hedge:test --tests "io.github.resilience4j.hedge.internal.InMemoryHedgeRegistryTest.shouldCascadeCloseToAllHedges" -i`
Expected: FAIL — `close()` not defined on HedgeRegistry

- [ ] **Step 3: Modify HedgeRegistry interface**

In `resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/HedgeRegistry.java`, change line 31 from:

```java
public interface HedgeRegistry extends Registry<Hedge, HedgeConfig> {
```

to:

```java
public interface HedgeRegistry extends Registry<Hedge, HedgeConfig>, AutoCloseable {
```

Add `close()` method at the end of the interface (before the closing `}`):

```java
    /**
     * Closes all managed Hedge instances by shutting down their internal schedulers.
     * <p>
     * This method is idempotent.
     */
    @Override
    void close();
```

- [ ] **Step 4: Implement cascade close in InMemoryHedgeRegistry**

In `resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/internal/InMemoryHedgeRegistry.java`, add at the end of the class (before closing `}`):

```java
    @Override
    public void close() {
        for (Hedge hedge : getAllHedges().collect(java.util.stream.Collectors.toList())) {
            hedge.close();
        }
    }
```

Add this import at the top if needed:
```java
import java.util.stream.Collectors;
```

Note: `Collectors.toList()` creates a defensive copy to avoid ConcurrentModificationException.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :resilience4j-hedge:test --tests "io.github.resilience4j.hedge.internal.InMemoryHedgeRegistryTest" -i`
Expected: ALL PASS

- [ ] **Step 6: Run all hedge module tests**

Run: `./gradlew :resilience4j-hedge:test -i`
Expected: ALL PASS

- [ ] **Step 7: Commit**

```bash
git add resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/HedgeRegistry.java resilience4j-hedge/src/main/java/io/github/resilience4j/hedge/internal/InMemoryHedgeRegistry.java resilience4j-hedge/src/test/java/io/github/resilience4j/hedge/internal/InMemoryHedgeRegistryTest.java
git commit -m "feat(hedge): add AutoCloseable cascade close to HedgeRegistry

HedgeRegistry extends AutoCloseable. InMemoryHedgeRegistry.close()
cascades to all managed Hedge instances. Follows ThreadPoolBulkhead
registry pattern. Part of #2451."
```

---

### Task 6: Full regression test

**Files:** None — validation only

- [ ] **Step 1: Run full hedge module tests**

Run: `./gradlew :resilience4j-hedge:test -i`
Expected: ALL PASS

- [ ] **Step 2: Run full core module tests**

Run: `./gradlew :resilience4j-core:test -i`
Expected: ALL PASS

- [ ] **Step 3: Run bulkhead module tests (verify NamingThreadFactory default unchanged)**

Run: `./gradlew :resilience4j-bulkhead:test -i`
Expected: ALL PASS — BulkheadNamingThreadFactory inherits `NamingThreadFactory(String)` which defaults to daemon=false

- [ ] **Step 4: Compile full project**

Run: `./gradlew assemble`
Expected: BUILD SUCCESSFUL

---

### Task 7: Final commit with design doc

- [ ] **Step 1: Add design doc and plan to git**

```bash
git add docs/superpowers/specs/2026-05-27-hedge-lifecycle-design.md docs/superpowers/plans/2026-05-27-hedge-lifecycle.md
git commit -m "docs: add design spec and implementation plan for #2451"
```

---

## Self-Review Checklist

**1. Spec coverage:**
- [x] Hedge extends AutoCloseable → Task 3 + Task 4
- [x] HedgeRegistry extends AutoCloseable with cascade → Task 5
- [x] NamingThreadFactory daemon constructor → Task 1
- [x] ContextAwareScheduledThreadPoolExecutor daemon flag → Task 2
- [x] HedgeImpl.close() with shutdown/await/shutdownNow → Task 4
- [x] daemon=true on Hedge scheduler → Task 4
- [x] Test: reject-after-close → Task 4
- [x] Test: graceful drain → Task 4
- [x] Test: idempotent close → Task 4
- [x] Test: scheduled-but-not-started on close → Task 4
- [x] Test: registry cascade → Task 5
- [x] Test: daemon thread verification → Task 1 + Task 2

**2. Placeholder scan:** No TBD, TODO, "implement later", or "add appropriate error handling" found.

**3. Type consistency:** `NamingThreadFactory(String, boolean)` used in Task 2 (via `new NamingThreadFactory(THREAD_PREFIX, daemon)`) and defined in Task 1. `daemon` field name consistent across Builder and constructor. `close()` signature consistent across Hedge interface and HedgeImpl.
