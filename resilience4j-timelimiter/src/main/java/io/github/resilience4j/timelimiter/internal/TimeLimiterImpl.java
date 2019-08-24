package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeLimiterImpl implements TimeLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(TimeLimiterImpl.class);

    private String name;
    private final TimeLimiterConfig timeLimiterConfig;
    private final LongAdder successes;
    private final LongAdder errors;
    private final LongAdder timeouts;
    private final TimeLimiterMetrics metrics;
    private final TimeLimiterEventProcessor eventProcessor;

    public TimeLimiterImpl(String name, TimeLimiterConfig timeLimiterConfig) {
        this.name = name;
        this.timeLimiterConfig = timeLimiterConfig;
        this.successes = new LongAdder();
        this.errors = new LongAdder();
        this.timeouts = new LongAdder();
        this.metrics = new TimeLimiterMetrics();
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
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    @Override
    public void onSuccess() {
        successes.increment();
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
        timeouts.increment();
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        publishEvent(new TimeLimiterOnTimeoutEvent(name));
    }

    private void onFailure(Throwable throwable) {
        errors.increment();
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        publishEvent(new TimeLimiterOnErrorEvent(name, throwable));
    }

    private void publishEvent(TimeLimiterEvent event) {
        try{
            eventProcessor.consumeEvent(event);
            LOG.debug("Event {} published: {}", event.getEventType(), event);
        } catch (Throwable t) {
            LOG.warn("Failed to handle event {}", event.getEventType(), t);
        }
    }

    public class TimeLimiterMetrics implements Metrics {

        @Override
        public long getNumberOfSuccessfulCalls() {
            return successes.longValue();
        }

        @Override
        public long getNumberOfErrorCalls() {
            return errors.longValue();
        }

        @Override
        public long getNumberOfTimedOutCalls() {
            return timeouts.longValue();
        }
    }
}