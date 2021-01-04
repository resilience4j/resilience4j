package io.github.resilience4j.common;

import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.core.functions.Either;

public class TestIntervalBiFunction implements IntervalBiFunction<Object> {
    @Override
    public Long apply(Integer integer, Either<Throwable, Object> objects) {
        return 200L;
    }
}
