package io.github.resilience4j.fallback;

import io.vavr.CheckedFunction0;

import java.time.Duration;
import java.util.concurrent.*;

import static java.time.Duration.ofNanos;

public class FutureFallbackDecorator implements FallbackDecorator{

    @Override
    public boolean supports(Class<?> target) {
        return Future.class.isAssignableFrom(target);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CheckedFunction0<Object> decorate(FallbackMethod fallbackMethod,
        CheckedFunction0<Object> supplier) {
        return supplier.andThen(request -> new FallbackFutureDecorator((Future) request, fallbackMethod));
    }

    static class FallbackFutureDecorator<T> implements Future<T> {

        private final Future<T> future;
        private final FallbackMethod fallbackMethod;

        FallbackFutureDecorator(Future<T> future,
            FallbackMethod fallbackMethod) {
            this.future = future;
            this.fallbackMethod = fallbackMethod;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                return future.get();
            } catch (CancellationException | InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                try {
                    return ((Future<T>) fallbackMethod.fallback(e)).get();
                } catch (Throwable throwable) {
                    return sneakyThrow(throwable);
                }
            }
        }

        @Override
        public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {

            //Split the time between actual get and fallback
            Duration timeoutDivBy2 = ofNanos(unit.toNanos(timeout)).dividedBy(2);
            try {
                return future.get(timeoutDivBy2.toNanos(),TimeUnit.NANOSECONDS);
            } catch (CancellationException | InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                try {
                    return ((Future<T>) fallbackMethod.fallback(e)).get(timeoutDivBy2.toNanos(),TimeUnit.NANOSECONDS);
                } catch (Throwable throwable) {
                    return sneakyThrow(throwable);
                }
            }
        }

        static <T extends Throwable, R> R sneakyThrow(Throwable t) throws T {
            throw (T) t;
        }
    }
}
