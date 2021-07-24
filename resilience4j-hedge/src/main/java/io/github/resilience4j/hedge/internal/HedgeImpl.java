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

import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.HedgeMetrics;
import io.github.resilience4j.hedge.event.*;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class HedgeImpl implements Hedge {

    private static final Logger LOG = LoggerFactory.getLogger(HedgeImpl.class);

    private final String name;
    private final Map<String, String> tags;
    private final HedgeConfig hedgeConfig;
    private final HedgeEventProcessor eventProcessor;
    private final HedgeMetrics metrics;

    public HedgeImpl(String name, HedgeConfig hedgeConfig) {
        this(name, hedgeConfig, HashMap.empty());
    }

    public HedgeImpl(String name, HedgeConfig hedgeConfig,
                     io.vavr.collection.Map<String, String> tags) {
        this.name = name;
        this.tags = Objects.requireNonNull(tags, "Tags must not be null");
        this.hedgeConfig = hedgeConfig;
        this.eventProcessor = new HedgeEventProcessor();
        this.metrics = hedgeConfig.newMetrics();
        if (metrics.requiresInput()) {
            eventProcessor.onPrimarySuccess(metrics::accept);
            eventProcessor.onPrimaryFailure(metrics::accept);
            eventProcessor.onHedgeSuccess(metrics::accept);
            eventProcessor.onHedgeFailure(metrics::accept);
        }
    }

    @Override
    public HedgeMetrics getMetrics() {
        return metrics;
    }

    private <T> CompletableFuture<T> callableFuture(Callable<T> callable, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable, ExecutorService primaryExecutor) {
        if (primaryExecutor instanceof ScheduledExecutorService) {
            return decorateCompletionStage(() -> callableFuture(callable, primaryExecutor), (ScheduledExecutorService) primaryExecutor).get().toCompletableFuture();
        } else {
            return decorateCompletionStage(() -> callableFuture(callable, primaryExecutor)).get().toCompletableFuture();
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable, ExecutorService primaryExecutor, ScheduledExecutorService hedgedExecutor) {
        return decorateCompletionStage(() -> callableFuture(callable, primaryExecutor), hedgedExecutor).get().toCompletableFuture();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(
        Supplier<F> supplier, ScheduledExecutorService hedgeExecutor) {
        return () -> {
            long start = System.nanoTime();
            CompletableFuture<HedgeResult<T>> supplied = supplier.get().toCompletableFuture()
                .handle((t, throwable) -> HedgeResult.of(t, true, throwable != null, throwable));
            CompletableFuture<T> timedCompletable = new CompletableFuture<>();
            ScheduledFuture<Boolean> sf = hedgeExecutor.schedule(() -> timedCompletable.complete(null), metrics.getResponseTimeCutoff().toMillis(), TimeUnit.MILLISECONDS);
            CompletableFuture<HedgeResult<T>> hedged = timedCompletable
                .thenCompose(t -> supplier.get())
                .handle((t, throwable) -> HedgeResult.of(t, false, throwable != null, throwable));
            return CompletableFuture.anyOf(hedged, supplied)
                .thenApply(s -> {
                    HedgeResult<T> t = (HedgeResult<T>) s;
                    long duration = System.nanoTime() - start;
                    if (t.fromPrimary) {
                        sf.cancel(true);
                        hedged.cancel(false);
                        if (t.failed) {
                            onPrimaryFailure(Duration.ofNanos(duration), t.throwable);
                            if (t.throwable instanceof RuntimeException) {
                                throw (RuntimeException) t.throwable;
                            }
                        } else {
                            onPrimarySuccess(Duration.ofNanos(duration));
                        }
                    } else {
                        supplied.cancel(false);
                        if (t.failed) {
                            onHedgedFailure(Duration.ofNanos(duration), t.throwable);
                            if (t.throwable instanceof RuntimeException) {
                                throw (RuntimeException) t.throwable;
                            }
                        } else {
                            onHedgedSuccess(Duration.ofNanos(duration));
                        }
                    }
                    return t.value;
                });
        };
    }

    @SuppressWarnings("unchecked")
    public <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(
        Supplier<F> supplier) {
        return () -> {
            long start = System.nanoTime();
            CompletableFuture<HedgeResult<T>> supplied = supplier.get().toCompletableFuture()
                .handle((t, throwable) -> HedgeResult.of(t, true, throwable != null, throwable));
            CompletableFuture<HedgeResult<T>> hedged = delayingStage(metrics.getResponseTimeCutoff().toMillis())
                .thenCompose(t -> supplier.get())
                .handle((t, throwable) -> HedgeResult.of(t, false, throwable != null, throwable));
            return CompletableFuture.anyOf(hedged, supplied)
                .thenApply(s -> {
                    HedgeResult<T> t = (HedgeResult<T>) s;
                    long duration = System.nanoTime() - start;
                    if (t.fromPrimary) {
                        hedged.cancel(false);
                        if (t.failed) {
                            onPrimaryFailure(Duration.ofNanos(duration), t.throwable);
                            throw (RuntimeException) t.throwable;
                        } else {
                            onPrimarySuccess(Duration.ofNanos(duration));
                            return t.value;
                        }
                    } else {
                        supplied.cancel(false);
                        if (t.failed) {
                            onHedgedFailure(Duration.ofNanos(duration), t.throwable);
                            throw (RuntimeException) t.throwable;
                        } else {
                            onHedgedSuccess(Duration.ofNanos(duration));
                            return t.value;
                        }
                    }
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
    public HedgeConfig getHedgeConfig() {
        return hedgeConfig;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    @Override
    public void onPrimarySuccess(Duration duration) {
        if (eventProcessor.hasConsumers()) {
            publishEvent(new PrimarySuccessEvent(name, duration));
        }
    }

    @Override
    public void onHedgedSuccess(Duration duration) {
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeSuccessEvent(name, duration));
        }
    }

    @Override
    public void onPrimaryFailure(Duration duration, Throwable throwable) {
        if (eventProcessor.hasConsumers()) {
            publishEvent(new PrimaryFailureEvent(name, duration, throwable));
        }
    }

    @Override
    public void onHedgedFailure(Duration duration, Throwable throwable) {
        if (eventProcessor.hasConsumers()) {
            publishEvent(new HedgeFailureEvent(name, duration, throwable));
        }
    }

    private void publishEvent(HedgeEvent event) {
        try {
            eventProcessor.consumeEvent(event);
        } catch (Exception e) {
            LOG.warn("Failed to handle event {}", event.getEventType(), e);
        }
    }

    private <T> CompletableFuture<T> delayingStage(long millis) {
        return CompletableFuture.supplyAsync(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(millis);
                    return null;
                } catch (InterruptedException e) {
                    return null;
                }
            }
        );
    }

}

