package io.github.resilience4j.core.predicate;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public class PredicateCreator {

    private PredicateCreator() {
    }

    @SafeVarargs
    public static Optional<Predicate<Throwable>> createExceptionsPredicate(
        Predicate<Throwable> exceptionPredicate,
        Class<? extends Throwable>... exceptions) {
        return PredicateCreator.createExceptionsPredicate(exceptions)
            .map(predicate -> exceptionPredicate == null ? predicate : predicate.or(exceptionPredicate))
            .or(() -> Optional.ofNullable(exceptionPredicate));
    }

    @SafeVarargs
    public static Optional<Predicate<Throwable>> createExceptionsPredicate(
        Class<? extends Throwable>... recordExceptions) {
        return exceptionPredicate(recordExceptions);
    }

    @SafeVarargs
    public static Optional<Predicate<Throwable>> createNegatedExceptionsPredicate(
        Class<? extends Throwable>... ignoreExceptions) {
        return exceptionPredicate(ignoreExceptions)
            .map(Predicate::negate);
    }

    private static Optional<Predicate<Throwable>> exceptionPredicate(
        Class<? extends Throwable>[] recordExceptions) {
        return Arrays.stream(recordExceptions)
            .distinct()
            .map(PredicateCreator::makePredicate)
            .reduce(Predicate::or);
    }

    private static Predicate<Throwable> makePredicate(Class<? extends Throwable> exClass) {
        return (Throwable e) -> exClass.isAssignableFrom(e.getClass());
    }
}
