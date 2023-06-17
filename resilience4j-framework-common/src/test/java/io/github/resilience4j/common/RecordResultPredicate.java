package io.github.resilience4j.common;

import java.util.function.Predicate;

public class RecordResultPredicate implements Predicate<String> {

    @Override
    public boolean test(String object) {
        return object.equals("failure");
    }
}
