package io.github.resilience4j.core;

import io.vavr.control.Either;

import java.util.function.Function;

public class ResultUtils {

    public static <T> boolean isSuccessfulAndReturned(
        Either<? extends Throwable, ?> callsResult,
        Class<T> expectedClass,
        Function<T, Boolean> returnedChecker) {
        if (callsResult.isLeft()) {
            return false;
        }
        Object result = callsResult.get();
        if (result == null) {
            return false;
        }
        if (!expectedClass.isAssignableFrom(result.getClass())) {
            return false;
        }
        return returnedChecker.apply((T) result);
    }

    public static <T extends Throwable>  boolean isFailedAndThrown(
        Either<? extends Throwable, ?> callsResult,
        Class<T> expectedClass) {
        return isFailedAndThrown(callsResult, expectedClass, thrown -> true);
    }

    public static <T extends Throwable>  boolean isFailedAndThrown(
        Either<? extends Throwable, ?> callsResult,
        Class<T> expectedClass,
        Function<T, Boolean> thrownChecker) {
        if (callsResult.isRight()) {
            return false;
        }
        Throwable thrown = callsResult.getLeft();
        if (!expectedClass.isAssignableFrom(thrown.getClass())) {
            return false;
        }
        return thrownChecker.apply((T) thrown);
    }
}
