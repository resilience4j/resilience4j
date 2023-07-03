package io.github.resilience4j.commons.configuration.dummy;

import java.util.function.Predicate;

public class DummyPredicateObject implements Predicate<Object> {
    @Override
    public boolean test(Object o) {
        return false;
    }
}
