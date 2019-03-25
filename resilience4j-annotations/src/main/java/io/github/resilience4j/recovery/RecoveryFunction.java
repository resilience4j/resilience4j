package io.github.resilience4j.recovery;

import io.vavr.CheckedFunction1;

public interface RecoveryFunction<R> extends CheckedFunction1<Throwable, R> {
}
