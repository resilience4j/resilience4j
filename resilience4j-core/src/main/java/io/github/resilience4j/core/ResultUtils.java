/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.core;

import io.github.resilience4j.core.functions.Either;

import java.util.function.Function;

public class ResultUtils {

    private ResultUtils() {
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
