package io.github.resilience4j.springboot.circuitbreaker;

import java.io.IOException;
import java.util.function.Predicate;

public class RecordFailurePredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        return throwable instanceof IOException || throwable instanceof IgnoredException;
    }
}
