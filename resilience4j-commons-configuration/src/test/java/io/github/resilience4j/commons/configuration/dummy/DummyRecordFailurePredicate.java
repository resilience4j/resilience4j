package io.github.resilience4j.commons.configuration.dummy;

import java.io.IOException;
import java.util.function.Predicate;

public class DummyRecordFailurePredicate implements Predicate<Throwable> {
        @Override
        public boolean test(Throwable throwable) {
            return throwable instanceof IOException || throwable instanceof DummyIgnoredException;
        }
}
