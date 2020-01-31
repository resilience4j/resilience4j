/*
 *
 *  Copyright 2020: Robert Winkler
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

import io.vavr.CheckedFunction0;

import java.util.List;
import java.util.function.Function;

public class CheckFunctionUtils {

    private CheckFunctionUtils() {
    }


    /**
     * Returns a composed function that first executes the function and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param function the function which should be recovered from a certain exception
     * @param exceptionHandler the exception handler
     * @return a function composed of callable and exceptionHandler
     */
    public static <T> CheckedFunction0<T> recover(CheckedFunction0<T> function,
        Function<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return function.apply();
            } catch (Throwable throwable) {
                return exceptionHandler.apply(throwable);
            }
        };
    }

    /**
     * Returns a composed function that first executes the function and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param function the function which should be recovered from a certain exception
     * @param exceptionTypes the specific exception types that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of supplier and exceptionHandler
     */
    public static <T> CheckedFunction0<T> recover(CheckedFunction0<T> function,
        List<Class<? extends Throwable>> exceptionTypes,
        Function<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return function.apply();
            } catch (Exception exception) {
                if(exceptionTypes.stream().anyMatch(exceptionType -> exceptionType.isAssignableFrom(exception.getClass()))){
                    return exceptionHandler.apply(exception);
                }else{
                    throw exception;
                }
            }
        };
    }

    /**
     * Returns a composed function that first executes the function and optionally recovers from an
     * exception.
     *
     * @param <T>              return type of after
     * @param function the function which should be recovered from a certain exception
     * @param exceptionType the specific exception type that should be recovered
     * @param exceptionHandler the exception handler
     * @return a function composed of callable and exceptionHandler
     */
    public static <X extends Throwable, T> CheckedFunction0<T> recover(CheckedFunction0<T> function,
        Class<X> exceptionType,
        Function<Throwable, T> exceptionHandler) {
        return () -> {
            try {
                return function.apply();
            } catch (Throwable throwable) {
                if(exceptionType.isAssignableFrom(throwable.getClass())) {
                    return exceptionHandler.apply(throwable);
                }else{
                    throw throwable;
                }
            }
        };
    }
}
