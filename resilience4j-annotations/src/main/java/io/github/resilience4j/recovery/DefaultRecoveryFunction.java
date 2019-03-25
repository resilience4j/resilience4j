package io.github.resilience4j.recovery;

public class DefaultRecoveryFunction<R> implements RecoveryFunction<R> {
    @Override
    public R apply(Throwable throwable) throws Throwable {
        throw  throwable;
    }
}
