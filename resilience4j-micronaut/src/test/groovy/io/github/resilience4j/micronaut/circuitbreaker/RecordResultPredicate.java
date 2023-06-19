package io.github.resilience4j.micronaut.circuitbreaker;

import java.util.function.Predicate;

public class RecordResultPredicate implements Predicate<String> {

    @Override
    public boolean test(String text) {
        return text.equals("failure");
    }
}
