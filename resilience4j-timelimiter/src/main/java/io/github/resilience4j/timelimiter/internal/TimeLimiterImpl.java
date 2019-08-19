package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class TimeLimiterImpl implements TimeLimiter {

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
                onError(e);
                Throwable t = e.getCause();
                if (t == null) {
                    throw e;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
                throw (Exception) t;
            }
        };
    }

    private void publishTimeLimiterEvent(TimeLimiterEvent.Type eventType) {
        if (!eventProcessor.hasConsumers()) {
            return;
        }
        eventProcessor.consumeEvent(TimeLimiterEvent.of(name, eventType));

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
        publishTimeLimiterEvent(TimeLimiterEvent.Type.SUCCESS);
    }

    @Override
    public void onError(Exception e) {
        if (e instanceof TimeoutException) {
            publishTimeLimiterEvent(TimeLimiterEvent.Type.TIMEOUT);
        } else {
            publishTimeLimiterEvent(TimeLimiterEvent.Type.FAILURE);
        }
    }

}
