package io.github.resilience4j.springboot.service.test;

import io.github.resilience4j.core.ContextPropagator;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BeanContextPropagator implements ContextPropagator {

    @Override
    public Supplier<Optional> retrieve() {
        return () -> Optional.empty();
    }

    @Override
    public Consumer<Optional> copy() {
        return (t) -> {
        };
    }

    @Override
    public Consumer<Optional> clear() {
        return (t) -> {
        };
    }
}
