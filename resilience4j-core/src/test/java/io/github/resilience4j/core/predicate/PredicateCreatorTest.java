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
        then(predicate.test(new RuntimeException())).isEqualTo(true);
        then(predicate.test(new IllegalArgumentException())).isEqualTo(true);
        then(predicate.test(new Throwable())).isEqualTo(false);
        then(predicate.test(new Exception())).isEqualTo(false);
        then(predicate.test(new IOException())).isEqualTo(true);

    }

    @Test
    public void buildIgnoreExceptionsPredicate() {
        Optional<Predicate<Throwable>> optionalPredicate = PredicateCreator
            .createNegatedExceptionsPredicate(RuntimeException.class, BusinessException.class);

        then(optionalPredicate).isNotEmpty();
        Predicate<Throwable> predicate = optionalPredicate.get();
        then(predicate.test(new RuntimeException())).isEqualTo(false);
        then(predicate.test(new IllegalArgumentException())).isEqualTo(false);
        then(predicate.test(new Throwable())).isEqualTo(true);
        then(predicate.test(new Exception())).isEqualTo(true);
        then(predicate.test(new IOException())).isEqualTo(true);
        then(predicate.test(new BusinessException())).isEqualTo(false);
    }

    private class BusinessException extends Exception {

    }
}
