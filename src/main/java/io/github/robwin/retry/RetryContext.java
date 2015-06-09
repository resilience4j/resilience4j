package io.github.robwin.retry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RetryContext implements Retry {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final int DEFAULT_WAIT_INTERVAL = 500;

    private final AtomicInteger numOfAttempts;
    private AtomicReference<Exception> lastException;
    private AtomicReference<RuntimeException> lastRuntimeException;


    // The maximum number of attempts
    private final int maxAttempts;
    // The wait interval between successive attempts
    private final int waitInterval;
    // Exceptions which should not trigger a retry
    private final List<Class<? extends Throwable>> ignoredExceptions;

    private RetryContext(int maxAttempts, int waitInterval, List<Class<? extends Throwable>> ignoredExceptions){
        this.maxAttempts = maxAttempts;
        this.waitInterval = waitInterval;
        this.ignoredExceptions = ignoredExceptions;
        this.numOfAttempts = new AtomicInteger(0);
        this.lastException = new AtomicReference<>();
        this.lastRuntimeException = new AtomicReference<>();
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    public boolean isRetryAllowedAfterException() throws Exception {
        int currentNumOfAttempts = numOfAttempts.incrementAndGet();
        if(currentNumOfAttempts == maxAttempts){
            throw lastException.get();
        }else{
            // wait interval until the next attempt should start
            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                throw lastException.get();
            }
            return true;
        }
    }
    @Override
    public boolean isRetryAllowedAfterRuntimeException(){
        int currentNumOfAttempts = numOfAttempts.incrementAndGet();
        if(currentNumOfAttempts == maxAttempts){
            throw lastRuntimeException.get();
        }else{
            // wait interval until the next attempt should start
            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                throw lastRuntimeException.get();
            }
            return true;
        }
    }


    @Override
    public void handleException(Exception exception) throws Throwable{
        if(ignoredExceptions.stream()
                .noneMatch(ignoredException -> ignoredException.isInstance(exception))){
            lastException.set(exception);
        }else{
            throw exception;
        }
    }

    @Override
    public void handleRuntimeException(RuntimeException runtimeException){
        if(ignoredExceptions.stream()
                .noneMatch(ignoredException -> ignoredException.isInstance(runtimeException))){
            lastRuntimeException.set(runtimeException);
        }else{
            throw runtimeException;
        }
    }

    public List<Class<? extends Throwable>> getIgnoredExceptions() {
        return ignoredExceptions;
    }

    public static class Builder {
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private int waitInterval = DEFAULT_WAIT_INTERVAL;
        private List<Class<? extends Throwable>> ignoredExceptions = new ArrayList<>();

        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder waitInterval(int waitInterval) {
            if (waitInterval < 10) {
                throw new IllegalArgumentException("waitInterval must be at least than 10[ms]");
            }
            this.waitInterval = waitInterval;
            return this;
        }

        public Builder ignoredException(Class<? extends Throwable> ignoredException) {
            if (ignoredException == null) {
                throw new IllegalArgumentException("ignoredException must not be null");
            }
            ignoredExceptions.add(ignoredException);
            return this;
        }

        public Builder ignoredExceptions(List<Class<? extends Throwable>> ignoredExceptions) {
            if (ignoredExceptions == null) {
                throw new IllegalArgumentException("ignoredExceptions must not be null");
            }
            this.ignoredExceptions = ignoredExceptions;
            return this;
        }

        public Retry build() {
            return new RetryContext(maxAttempts, waitInterval, ignoredExceptions);
        }
    }
}
