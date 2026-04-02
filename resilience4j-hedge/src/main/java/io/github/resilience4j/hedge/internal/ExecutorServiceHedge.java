package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.hedge.GenericHedge;
import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.SimpleHedgeConfig;
import io.github.resilience4j.hedge.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class ExecutorServiceHedge<E extends ScheduledExecutorService> implements GenericHedge {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorServiceHedge.class);
    protected final String name;
    protected final Map<String, String> tags;
    protected final SimpleHedgeConfig hedgeConfig;
    protected final HedgeEventProcessor eventProcessor;
    protected final HedgeDurationSupplier durationSupplier;

    protected final E configuredHedgeExecutor;

    public ExecutorServiceHedge(String name, Map<String, String> tags, SimpleHedgeConfig hedgeConfig, @NonNull E scheduledExecutorService) {
        this.name = name;
        this.tags = Objects.requireNonNull(tags, "Tags must not be null");
        this.hedgeConfig = hedgeConfig;
        this.eventProcessor = new HedgeEventProcessor();
        this.durationSupplier = HedgeDurationSupplier.fromConfig(hedgeConfig);
        this.configuredHedgeExecutor = scheduledExecutorService;
    }

    private static <T> CompletableFuture<T> callableFuture(Callable<T> callable, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    @Override
    public HedgeDurationSupplier getDurationSupplier() {
        return durationSupplier;
    }

    @Override
    public Duration getDuration() {
        return durationSupplier.get();
    }

    public <T> CompletableFuture<T> submit(Callable<T> callable, ExecutorService primaryExecutor) {
        return decorateCaller(() -> ExecutorServiceHedge.callableFuture(callable, primaryExecutor), () -> ExecutorServiceHedge.callableFuture(callable, configuredHedgeExecutor))
                .get()
                .toCompletableFuture();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(Supplier<F> supplier) {
        return decorateCaller(supplier, supplier);
    }

    private <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCaller(Supplier<F> primarySupplier, Supplier<F> hedgedSupplier) {
        return () -> {
            long start = System.nanoTime();
            CompletableFuture<HedgeResult<T>> supplied = primarySupplier.get().toCompletableFuture()
                    .handle((t, throwable) -> HedgeResult.of(t, true, Optional.ofNullable(throwable)));
            CompletableFuture<T> timedCompletable = new CompletableFuture<>();
            CompletableFuture<HedgeResult<T>> hedged = timedCompletable
                    .thenCompose(t -> hedgedSupplier.get())
                    .handle((t, throwable) -> HedgeResult.of(t, false, Optional.ofNullable(throwable)));
            ScheduledFuture<Boolean> sf = configuredHedgeExecutor.schedule(() -> timedCompletable.complete(null), durationSupplier.get().toNanos(), TimeUnit.NANOSECONDS);
            return CompletableFuture.anyOf(hedged, supplied)
                    .thenApply(s -> {
                        HedgeResult<T> t = (HedgeResult<T>) s;
                        long duration = System.nanoTime() - start;
                        if (t.fromPrimary) {
                            sf.cancel(true);
                            hedged.cancel(false);
                            if (t.throwable.isPresent()) {
                                onPrimaryFailure(Duration.ofNanos(duration), t.throwable.get());
                                throw (RuntimeException) t.throwable.get();
                            } else {
                                onPrimarySuccess(Duration.ofNanos(duration));
                            }
                        } else {
                            supplied.cancel(false);
                            if (t.throwable.isPresent()) {
                                onSecondaryFailure(Duration.ofNanos(duration), t.throwable.get());
                                throw (RuntimeException) t.throwable.get();
                            } else {
                                onSecondarySuccess(Duration.ofNanos(duration));
                            }
                        }
                        return t.value;
                    });
        };
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public SimpleHedgeConfig getHedgeConfig() {
        return hedgeConfig;
    }

    @Override
    public Hedge.EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    public void onPrimarySuccess(Duration duration) {
        durationSupplier.accept(HedgeEvent.Type.PRIMARY_SUCCESS, duration);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeOnPrimarySuccessEvent(name, duration));
        }
    }

    public void onSecondarySuccess(Duration duration) {
        durationSupplier.accept(HedgeEvent.Type.SECONDARY_SUCCESS, duration);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeOnSecondarySuccessEvent(name, duration));
        }
    }

    public void onPrimaryFailure(Duration duration, Throwable throwable) {
        durationSupplier.accept(HedgeEvent.Type.PRIMARY_FAILURE, duration);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeOnPrimaryFailureEvent(name, duration, throwable));
        }
    }

    public void onSecondaryFailure(Duration duration, Throwable throwable) {
        durationSupplier.accept(HedgeEvent.Type.SECONDARY_FAILURE, duration);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeOnSecondaryFailureEvent(name, duration, throwable));
        }
    }

    private void publishEvent(HedgeEvent event) {
        try {
            eventProcessor.consumeEvent(event);
        } catch (RuntimeException e) {
            LOG.warn("Failed to handle event {}", event.getEventType(), e);
        }
    }

    public E getConfiguredHedgeExecutor() {
        return configuredHedgeExecutor;
    }
}
