package io.github.resilience4j.springboot;

import io.github.resilience4j.core.ContextPropagator;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.resilience4j.springboot.TestThreadLocalContextPropagator.TestThreadLocalContextHolder.*;

public class TestThreadLocalContextPropagator<T> implements ContextPropagator<T> {

    @Override
    public Supplier<Optional<T>> retrieve() {
        return () -> (Optional<T>) get();
    }

    @Override
    public Consumer<Optional<T>> copy() {
        return (t) -> t.ifPresent(e -> {
            clear();
            put(e);
        });
    }

    @Override
    public Consumer<Optional<T>> clear() {
        return (t) -> TestThreadLocalContextHolder.clear();
    }

    public static class TestThreadLocalContextHolder {

        private static final ThreadLocal threadLocal = new ThreadLocal();

        private TestThreadLocalContextHolder() {
        }

        public static void put(Object context) {
            if (threadLocal.get() != null) {
                clear();
            }
            threadLocal.set(context);
        }

        public static void clear() {
            if (threadLocal.get() != null) {
                threadLocal.set(null);
                threadLocal.remove();
            }
        }

        public static Optional<Object> get() {
            return Optional.ofNullable(threadLocal.get());
        }
    }
}

