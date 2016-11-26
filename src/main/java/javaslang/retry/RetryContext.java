package javaslang.retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class RetryContext implements Retry {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_WAIT_DURATION = 500;

    private final AtomicInteger numOfAttempts;
    private AtomicReference<Exception> lastException;
    private AtomicReference<RuntimeException> lastRuntimeException;

    // The maximum number of attempts
    private final int maxAttempts;
    // The wait interval between successive attempts
    private final long waitDuration;
    private Predicate<Throwable> exceptionPredicate;

    private RetryContext(int maxAttempts, Duration waitDuration, Predicate<Throwable> exceptionPredicate){
        this.maxAttempts = maxAttempts;
        this.waitDuration = waitDuration.getSeconds() * 1000;
        this.exceptionPredicate = exceptionPredicate;
        this.numOfAttempts = new AtomicInteger(0);
        this.lastException = new AtomicReference<>();
        this.lastRuntimeException = new AtomicReference<>();
    }

    @Override
    public void throwOrSleepAfterException() throws Exception {
        int currentNumOfAttempts = numOfAttempts.incrementAndGet();
        if(currentNumOfAttempts == maxAttempts){
            throw lastException.get();
        }else{
            // wait interval until the next attempt should start
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException e) {
                throw lastException.get();
            }
        }
    }
    @Override
    public void throwOrSleepAfterRuntimeException(){
        int currentNumOfAttempts = numOfAttempts.incrementAndGet();
        if(currentNumOfAttempts == maxAttempts){
            throw lastRuntimeException.get();
        }else{
            // wait interval until the next attempt should start
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException e) {
                throw lastRuntimeException.get();
            }
        }
    }

    @Override
    public void handleException(Exception exception) throws Throwable{
        if(exceptionPredicate.test(exception)){
            lastException.set(exception);
        }else{
            throw exception;
        }
    }

    @Override
    public void handleRuntimeException(RuntimeException runtimeException){
        if(exceptionPredicate.test(runtimeException)){
            lastRuntimeException.set(runtimeException);
        }else{
            throw runtimeException;
        }
    }

    public static class Builder {
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private Duration waitDuration = Duration.ofMillis(DEFAULT_WAIT_DURATION);
        // The default exception predicate retries all exceptions.
        private Predicate<Throwable> exceptionPredicate = (exception) -> true;

        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder waitDuration(Duration waitDuration) {
            if (waitDuration.toMillis() < 10) {
                throw new IllegalArgumentException("waitDurationInOpenState must be at least 10ms");
            }
            this.waitDuration = waitDuration;
            return this;
        }

        /**
         *  Configures a Predicate which evaluates if an exception should be retried.
         *  The Predicate must return true if the exception should count be retried, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an exception should be retried or not.
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder retryOnException(Predicate<Throwable> predicate) {
            this.exceptionPredicate = predicate;
            return this;
        }

        public Retry build() {
            return new RetryContext(maxAttempts, waitDuration, exceptionPredicate);
        }
    }
}
