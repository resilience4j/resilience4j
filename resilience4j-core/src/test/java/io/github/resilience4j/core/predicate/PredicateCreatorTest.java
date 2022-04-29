package io.github.resilience4j.core.predicate;

import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.BDDAssertions.then;

public class PredicateCreatorTest {

    @Test
    public void buildRecordExceptionsPredicate() {
        Optional<Predicate<Throwable>> optionalPredicate = PredicateCreator
            .createExceptionsPredicate(RuntimeException.class, IOException.class);

        then(optionalPredicate).isNotEmpty();
        Predicate<Throwable> predicate = optionalPredicate.get();
        then(predicate.test(new RuntimeException())).isTrue();
        then(predicate.test(new IllegalArgumentException())).isTrue();
        then(predicate.test(new Throwable())).isFalse();
        then(predicate.test(new Exception())).isFalse();
        then(predicate.test(new IOException())).isTrue();

    }

    @Test
    public void buildIgnoreExceptionsPredicate() {
        Optional<Predicate<Throwable>> optionalPredicate = PredicateCreator
            .createNegatedExceptionsPredicate(RuntimeException.class, BusinessException.class);

        then(optionalPredicate).isNotEmpty();
        Predicate<Throwable> predicate = optionalPredicate.get();
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
