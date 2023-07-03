package io.github.resilience4j.commons.configuration.dummy;

import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.core.functions.Either;

public class DummyIntervalBiFunction implements IntervalBiFunction<Object> {
    @Override
    public Long apply(Integer integer, Either<Throwable, Object> throwableObjectEither) {
        return null;
    }
}
