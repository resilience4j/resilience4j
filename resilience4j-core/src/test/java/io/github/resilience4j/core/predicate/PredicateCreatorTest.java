package io.github.resilience4j.core.predicate;

import org.junit.Test;

import java.io.IOException;
import java.util.function.Predicate;

import static org.assertj.core.api.BDDAssertions.then;

public class PredicateCreatorTest {

    @Test
    public void buildComplexRecordExceptionsPredicateOnlyClasses() {
        Predicate<Throwable> exceptionPredicate = null;

        Predicate<Throwable> predicate = PredicateCreator
            .createExceptionsPredicate(exceptionPredicate, IOException.class, RuntimeException.class)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isTrue();
        then(predicate.test(new IllegalArgumentException())).isTrue();
        then(predicate.test(new Throwable())).isFalse();
        then(predicate.test(new Exception())).isFalse();
        then(predicate.test(new IOException())).isTrue();

    }

    @Test
    public void buildComplexRecordExceptionsPredicateWithoutClasses() {
        Predicate<Throwable> exceptionPredicate = t -> t instanceof IOException || t instanceof RuntimeException;

        Predicate<Throwable> predicate = PredicateCreator
            .createExceptionsPredicate(exceptionPredicate)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isTrue();
        then(predicate.test(new IllegalArgumentException())).isTrue();
        then(predicate.test(new Throwable())).isFalse();
        then(predicate.test(new Exception())).isFalse();
        then(predicate.test(new IOException())).isTrue();

    }

    @Test
    public void buildComplexRecordExceptionsPredicate() {
        Predicate<Throwable> exceptionPredicate = t -> t instanceof IOException;

        Predicate<Throwable> predicate = PredicateCreator
            .createExceptionsPredicate(exceptionPredicate, RuntimeException.class)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isTrue();
        then(predicate.test(new IllegalArgumentException())).isTrue();
        then(predicate.test(new Throwable())).isFalse();
        then(predicate.test(new Exception())).isFalse();
        then(predicate.test(new IOException())).isTrue();

    }

    @Test
    public void buildRecordExceptionsPredicate() {
        Predicate<Throwable> predicate = PredicateCreator
            .createExceptionsPredicate(RuntimeException.class, IOException.class)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isTrue();
        then(predicate.test(new IllegalArgumentException())).isTrue();
        then(predicate.test(new Throwable())).isFalse();
        then(predicate.test(new Exception())).isFalse();
        then(predicate.test(new IOException())).isTrue();

    }

    @Test
    public void buildIgnoreExceptionsPredicate() {
        Predicate<Throwable> predicate = PredicateCreator
            .createNegatedExceptionsPredicate(RuntimeException.class, BusinessException.class)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isFalse();
        then(predicate.test(new IllegalArgumentException())).isFalse();
        then(predicate.test(new Throwable())).isTrue();
        then(predicate.test(new Exception())).isTrue();
        then(predicate.test(new IOException())).isTrue();
        then(predicate.test(new BusinessException())).isFalse();
    }

    private class BusinessException extends Exception {

    }
}
