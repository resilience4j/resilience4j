package io.github.resilience4j.recovery;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;

public interface RecoveryDecorator extends CheckedFunction1<CheckedFunction0<Object>, Object> {
}
