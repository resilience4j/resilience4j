package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.Clock;
import io.github.resilience4j.core.metrics.LockFreeSlidingTimeWindowMetrics;
import io.github.resilience4j.core.metrics.SlidingTimeWindowMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryBudgetExceededException;
import io.github.resilience4j.retry.RetryConfig;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BudgetRetryImpl implements Retry {
    private final Retry delegate;
    private final io.github.resilience4j.core.metrics.Metrics metrics;
    private final double maxRetryRatio;
    private final long minSampleSize;

    public BudgetRetryImpl(Retry delegate, RetryConfig config) {
        this.delegate = delegate;

        if(config.getSynchronizationStrategy() == SlidingWindowSynchronizationStrategy.LOCK_FREE) {
            this.metrics = new LockFreeSlidingTimeWindowMetrics(config.getWindowSize(), Clock.SYSTEM);
        } else {
            this.metrics = new SlidingTimeWindowMetrics(config.getWindowSize(), config.getClock());
        }

        this.maxRetryRatio = config.getMaxRetryRatio();
        this.minSampleSize = config.getMinSampleSize();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public <T> Context<T> context() {
        return new RetryBudgetContext<>(delegate.context());
    }

    @Override
    public <T> AsyncContext<T> asyncContext() {
        return new AsyncRetryBudgetContext<>(delegate.asyncContext());
    }

    @Override
    public RetryConfig getRetryConfig() {
        return delegate.getRetryConfig();
    }

    @Override
    public Map<String, String> getTags() {
        return delegate.getTags();
    }

    @Override
    public EventPublisher getEventPublisher() {
        return delegate.getEventPublisher();
    }

    @Override
    public Metrics getMetrics() {
        return delegate.getMetrics();
    }

    public io.github.resilience4j.core.metrics.Metrics getSlidingWindowMetrics() {
        return metrics;
    }

    private boolean allowRetry() {
        var snapshot = metrics.getSnapshot();
        long total = snapshot.getTotalNumberOfCalls();

        if (total <= minSampleSize) {
            return true; // warm-up phase
        }

        return snapshot.getFailureRate() <= maxRetryRatio;
    }

    public final class RetryBudgetContext<T> implements Retry.Context<T> {

        private final Retry.Context<T> delegate;

        public RetryBudgetContext(Retry.Context<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
            metrics.record(0, TimeUnit.MILLISECONDS, io.github.resilience4j.core.metrics.Metrics.Outcome.SUCCESS);
        }

        @Override
        public boolean onResult(T result) {
            boolean shouldRetry = delegate.onResult(result);

            metrics.record(
                    0,
                    TimeUnit.MILLISECONDS, shouldRetry ?
                    io.github.resilience4j.core.metrics.Metrics.Outcome.ERROR :
                    io.github.resilience4j.core.metrics.Metrics.Outcome.SUCCESS
            );

            return shouldRetry && allowRetry();
        }

        @Override
        public void onError(Exception exception) throws Exception {
            if (!allowRetry()) {
                throw new RetryBudgetExceededException(getName(), maxRetryRatio);
            }

            try {
                delegate.onError(exception);
                metrics.record(0, TimeUnit.MILLISECONDS, io.github.resilience4j.core.metrics.Metrics.Outcome.ERROR);
            } catch (Exception e) {
                metrics.record(0, TimeUnit.MILLISECONDS, io.github.resilience4j.core.metrics.Metrics.Outcome.ERROR);
                throw e;
            }
        }

        @Override
        public void onRuntimeError(RuntimeException runtimeException) {
            if (!allowRetry()) {
                throw new RetryBudgetExceededException(getName(), maxRetryRatio);
            }
            delegate.onRuntimeError(runtimeException);
            metrics.record(0, TimeUnit.MILLISECONDS, io.github.resilience4j.core.metrics.Metrics.Outcome.ERROR);
        }
    }

    public final class AsyncRetryBudgetContext<T> implements Retry.AsyncContext<T> {

        private final Retry.AsyncContext<T> delegate;

        public AsyncRetryBudgetContext(Retry.AsyncContext<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
            metrics.record(0, TimeUnit.MILLISECONDS,
                    io.github.resilience4j.core.metrics.Metrics.Outcome.SUCCESS);
        }

        @Override
        public long onError(Throwable throwable) {
            metrics.record(0, TimeUnit.MILLISECONDS,
                    io.github.resilience4j.core.metrics.Metrics.Outcome.ERROR);

            if (!allowRetry()) {
                return -1;
            }

            return delegate.onError(throwable);
        }

        @Override
        public long onResult(T result) {
            long delay = delegate.onResult(result);

            metrics.record(0, TimeUnit.MILLISECONDS,
                    delay >= 0
                            ? io.github.resilience4j.core.metrics.Metrics.Outcome.ERROR
                            : io.github.resilience4j.core.metrics.Metrics.Outcome.SUCCESS);

            if (delay >= 0 && !allowRetry()) {
                return -1;
            }

            return delay;
        }
    }
}
