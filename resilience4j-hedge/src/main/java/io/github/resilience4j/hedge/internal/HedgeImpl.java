/*
 *
 *  Copyright 2021: Matthew Sandoz
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

public class HedgeImpl implements Hedge {

    private static final Logger LOG = LoggerFactory.getLogger(HedgeImpl.class);

    private final String name;
    private final Map<String, String> tags;
    private final HedgeConfig hedgeConfig;
    private final HedgeEventProcessor eventProcessor;
    private final HedgeDurationSupplier durationSupplier;
    private final HedgeMetrics metrics;
    private final ContextAwareScheduledThreadPoolExecutor configuredHedgeExecutor;

    public HedgeImpl(String name, HedgeConfig hedgeConfig) {
        this(name, hedgeConfig, emptyMap());
    }

    public HedgeImpl(String name, HedgeConfig hedgeConfig,
                     Map<String, String> tags) {
        this.name = name;
        this.tags = Objects.requireNonNull(tags, "Tags must not be null");
        this.hedgeConfig = hedgeConfig;
        this.eventProcessor = new HedgeEventProcessor();
        this.durationSupplier = HedgeDurationSupplier.fromConfig(hedgeConfig);
        this.metrics = new HedgeMetrics();
        this.configuredHedgeExecutor =
            ContextAwareScheduledThreadPoolExecutor
                .newScheduledThreadPool()
                .corePoolSize(hedgeConfig.getConcurrentHedges())
                .contextPropagators(hedgeConfig.getContextPropagators())
                .build();
    }

    @Override
    public HedgeDurationSupplier getDurationSupplier() {
        return durationSupplier;
    }

    @Override
    public Duration getDuration() {
        return durationSupplier.get();
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
    public <T> CompletableFuture<T> submit(Callable<T> callable, ExecutorService primaryExecutor) {
        return decorateCaller(() -> callableFuture(callable, primaryExecutor), () -> callableFuture(callable, configuredHedgeExecutor))
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
    public Metrics getMetrics() {
        return metrics;
    }

    private final class HedgeMetrics implements Metrics {

        private AtomicLong primarySuccess = new AtomicLong(0);
        private AtomicLong primaryFailure = new AtomicLong(0);
        private AtomicLong secondarySuccess = new AtomicLong(0);
        private AtomicLong secondaryFailure = new AtomicLong(0);

        @Override
        public Duration getCurrentHedgeDelay() {
            return getDuration();
        }

        @Override
        public long getPrimarySuccessCount() {
            return primarySuccess.get();
        }

        @Override
        public long getSecondarySuccessCount() {
            return secondarySuccess.get();
        }

        @Override
        public long getPrimaryFailureCount() {
            return primaryFailure.get();
        }

        @Override
        public long getSecondaryFailureCount() {
            return secondaryFailure.get();
        }

        @Override
        public int getSecondaryPoolActiveCount() {
            return configuredHedgeExecutor.getActiveCount();
        }
    }

    @Override
    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public HedgeConfig getHedgeConfig() {
        return hedgeConfig;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    @Override
    public void onPrimarySuccess(Duration duration) {
        metrics.primarySuccess.incrementAndGet();
        durationSupplier.accept(HedgeEvent.Type.PRIMARY_SUCCESS, duration);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeOnPrimarySuccessEvent(name, duration));
        }
    }

    @Override
    public void onSecondarySuccess(Duration duration) {
        metrics.secondarySuccess.incrementAndGet();
        durationSupplier.accept(HedgeEvent.Type.SECONDARY_SUCCESS, duration);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeOnSecondarySuccessEvent(name, duration));
        }
    }

    @Override
    public void onPrimaryFailure(Duration duration, Throwable throwable) {
        metrics.primaryFailure.incrementAndGet();
        durationSupplier.accept(HedgeEvent.Type.PRIMARY_FAILURE, duration);
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeOnPrimaryFailureEvent(name, duration, throwable));
        }
    }

    @Override
    public void onSecondaryFailure(Duration duration, Throwable throwable) {
        metrics.secondaryFailure.incrementAndGet();
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

}

