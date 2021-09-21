package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;

public class TimeLimiterImpl implements TimeLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(TimeLimiterImpl.class);

    private final String name;
    private final Map<String, String> tags;
    private final TimeLimiterConfig timeLimiterConfig;
    private final TimeLimiterEventProcessor eventProcessor;

    public TimeLimiterImpl(String name, TimeLimiterConfig timeLimiterConfig) {
        this(name, timeLimiterConfig, emptyMap());

    }

    public TimeLimiterImpl(String name, TimeLimiterConfig timeLimiterConfig, Map<String, String> tags) {
        this.name = name;
        this.tags = Objects.requireNonNull(tags, "Tags must not be null");
        this.timeLimiterConfig = timeLimiterConfig;
        this.eventProcessor = new TimeLimiterEventProcessor();
    }


    @Override
    public <T, F extends Future<T>> Callable<T> decorateFutureSupplier(Supplier<F> futureSupplier) {
        return () -> {
            Future<T> future = futureSupplier.get();
            try {
                T result = future.get(getTimeLimiterConfig().getTimeoutDuration().toMillis(),
                    TimeUnit.MILLISECONDS);
                onSuccess();
                return result;
            } catch (TimeoutException e) {
                TimeoutException timeoutException = TimeLimiter.createdTimeoutExceptionWithName(name, e);
                onError(timeoutException);
                if (getTimeLimiterConfig().shouldCancelRunningFuture()) {
                    future.cancel(true);
                }
                throw timeoutException;
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t == null) {
                    onError(e);
                    throw e;
                }
                onError(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw (Exception) t;
            }
        };
    }

    @Override
    public <T, F extends CompletionStage<T>> Supplier<CompletionStage<T>> decorateCompletionStage(
        ScheduledExecutorService scheduler, Supplier<F> supplier) {

        return () -> {
            CompletableFuture<T> future = supplier.get().toCompletableFuture();
            ScheduledFuture<?> timeoutFuture =
                Timeout
                    .of(future, scheduler, name, getTimeLimiterConfig().getTimeoutDuration().toMillis(),
                        TimeUnit.MILLISECONDS);

            return future.whenComplete((result, throwable) -> {
                // complete
                if (result != null) {
                    if (!timeoutFuture.isDone()) {
                        timeoutFuture.cancel(false);
                    }
                    onSuccess();
                }

                // exceptionally
                if (throwable != null) {
                    if (throwable instanceof CompletionException) {
                        Throwable cause = throwable.getCause();
                        onError(cause);
                    } else if (throwable instanceof ExecutionException) {
                        Throwable cause = throwable.getCause();
                        if (cause == null) {
                            onError(throwable);
                        } else {
                            onError(cause);
                        }
                    } else {
                        onError(throwable);
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
    public TimeLimiterConfig getTimeLimiterConfig() {
        return timeLimiterConfig;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    @Override
    public void onSuccess() {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        publishEvent(new TimeLimiterOnSuccessEvent(name));
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable instanceof TimeoutException) {
            onTimeout();
        } else {
            onFailure(throwable);
        }
    }

    private void onTimeout() {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        publishEvent(new TimeLimiterOnTimeoutEvent(name));
    }

    private void onFailure(Throwable throwable) {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        publishEvent(new TimeLimiterOnErrorEvent(name, throwable));
    }

    private void publishEvent(TimeLimiterEvent event) {
        try {
            eventProcessor.consumeEvent(event);
            LOG.debug("Event {} published: {}", event.getEventType(), event);
        } catch (Exception e) {
            LOG.warn("Failed to handle event {}", event.getEventType(), e);
        }
    }

    /**
     * Completes CompletableFuture with {@link TimeoutException}.
     */
    static final class Timeout {

        private Timeout() {
        }

        static ScheduledFuture<?> of(
            CompletableFuture<?> future, ScheduledExecutorService scheduler, String name, long delay,
            TimeUnit unit) {
            return scheduler.schedule(() -> {
                if (future != null && !future.isDone()) {
                    future.completeExceptionally(TimeLimiter.createdTimeoutExceptionWithName(name, null));
                }
            }, delay, unit);
        }
    }
}