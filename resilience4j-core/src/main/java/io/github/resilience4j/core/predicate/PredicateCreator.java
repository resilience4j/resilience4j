package io.github.resilience4j.core.predicate;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public class PredicateCreator {

    @SafeVarargs
    public static Optional<Predicate<Throwable>> createRecordExceptionsPredicate(Class<? extends Throwable> ...recordExceptions) {
        return Arrays.stream(recordExceptions)
                .distinct()
                .map(PredicateCreator::makePredicate)
                .reduce(Predicate::or);
    }

    @SafeVarargs
    public static Optional<Predicate<Throwable>> createIgnoreExceptionsPredicate(Class<? extends Throwable> ...ignoreExceptions) {
        return Arrays.stream(ignoreExceptions)
                .distinct()
                .map(PredicateCreator::makePredicate)
                .reduce(Predicate::or)
                .map(Predicate::negate);
    }

    static private Predicate<Throwable> makePredicate(Class<? extends Throwable> exClass) {
        return (Throwable e) -> exClass.isAssignableFrom(e.getClass());
    }
}
