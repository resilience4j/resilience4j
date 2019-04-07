/*
 *
 *  Copyright 2019 Kyuhyen Hwang
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
package io.github.resilience4j.circuitbreaker;

import io.vavr.Function1;
import io.vavr.Function2;

/**
 * default result predication functions that always return true
 */
class CircuitBreakerResultPredicates {
    private static final Function1<Long, Boolean> SUCCESS_PREDICATE1 = (duration) -> true;
    private static final Function2<Long, Object, Boolean> SUCCESS_PREDICATE2 = (duration, result) -> true;

    static Function1<Long, Boolean> defaultPredicate1() {
        return SUCCESS_PREDICATE1;
    }

    @SuppressWarnings("unchecked")
    static <T> Function2<Long, T, Boolean> defaultPredicate2() {
        return (Function2<Long, T, Boolean>) SUCCESS_PREDICATE2;
    }
}
