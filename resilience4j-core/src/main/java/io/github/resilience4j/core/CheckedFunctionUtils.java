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

import io.github.resilience4j.core.functions.CheckedBiFunction;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedSupplier;

import java.util.List;
import java.util.function.Predicate;

public class CheckedFunctionUtils {

    private CheckedFunctionUtils() {
    }


    /**
     * @deprecated use {@link CheckedSupplierUtils#recover(CheckedSupplier, CheckedFunction)}
     */
    @Deprecated
    public static <T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
                                                 CheckedFunction<Throwable, T> exceptionHandler) {
        return CheckedSupplierUtils.recover(supplier, exceptionHandler);
    }

    /**
     * @deprecated use {@link CheckedSupplierUtils#andThen(CheckedSupplier, CheckedBiFunction)}
     */
    @Deprecated
    public static <T, R> CheckedSupplier<R> andThen(CheckedSupplier<T> supplier,
        CheckedBiFunction<T, Throwable, R> handler) {
        return CheckedSupplierUtils.andThen(supplier, handler);
    }

    /**
     * @deprecated use {@link CheckedSupplierUtils#recover(CheckedSupplier, Predicate, CheckedFunction)}
     */
    @Deprecated
    public static <T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
        Predicate<T> resultPredicate, CheckedFunction<T, T> resultHandler) {
        return CheckedSupplierUtils.recover(supplier, resultPredicate, resultHandler);
    }

    /**
     * @deprecated use {@link CheckedSupplierUtils#recover(CheckedSupplier, List, CheckedFunction)}
     */
    @Deprecated
    public static <T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
        List<Class<? extends Throwable>> exceptionTypes,
        CheckedFunction<Throwable, T> exceptionHandler) {
        return CheckedSupplierUtils.recover(supplier, exceptionTypes, exceptionHandler);
    }

    /**
     * @deprecated use {@link CheckedSupplierUtils#recover(CheckedSupplier, Class, CheckedFunction)}
     */
    public static <X extends Throwable, T> CheckedSupplier<T> recover(CheckedSupplier<T> supplier,
        Class<X> exceptionType,
        CheckedFunction<Throwable, T> exceptionHandler) {
        return CheckedSupplierUtils.recover(supplier, exceptionType, exceptionHandler);
    }
}
