package io.github.resilience4j.utils;

import io.github.resilience4j.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.recovery.RecoveryFunction;

public class RecoveryFunctionUtils {

    /**
     * create new instance of RecoveryFunction class. If the class is {@link DefaultRecoveryFunction}, then returns its singleton instance.
     */
    @SuppressWarnings("unchecked")
    public static <T extends RecoveryFunction<?>> T getInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException {
        return DefaultRecoveryFunction.class.isAssignableFrom(clazz) ? (T) DefaultRecoveryFunction.getInstance() : clazz.newInstance();
    }
}
