package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class TimeLimiterImpl implements TimeLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(TimeLimiterImpl.class);

    private String name;
    private final TimeLimiterConfig timeLimiterConfig;
    private final TimeLimiterEventProcessor eventProcessor;

    public TimeLimiterImpl(String name, TimeLimiterConfig timeLimiterConfig) {
        this.name = name;
        this.timeLimiterConfig = timeLimiterConfig;
        this.eventProcessor = new TimeLimiterEventProcessor();
    }

    @Override
    public <T, F extends Future<T>> Callable<T> decorateFutureSupplier(Supplier<F> futureSupplier) {
        return () -> {
            Future<T> future = futureSupplier.get();
            try {
                T result = future.get(getTimeLimiterConfig().getTimeoutDuration().toMillis(), TimeUnit.MILLISECONDS);
                onSuccess();
                return result;
            } catch (TimeoutException e) {
                onError(e);
                if (getTimeLimiterConfig().shouldCancelRunningFuture()) {
                    future.cancel(true);
                }
                throw e;
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
    public String getName() {
        return name;
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
}