/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.core.predicate;

import java.io.IOException;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class PredicateCreatorTest {

    @Test
    void buildComplexRecordExceptionsPredicateOnlyClasses() {
        Predicate<Throwable> exceptionPredicate = null;

        Predicate<Throwable> predicate = PredicateCreator
            .createExceptionsPredicate(exceptionPredicate, IOException.class, RuntimeException.class)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isTrue();
        then(predicate.test(new IllegalArgumentException())).isTrue();
        then(predicate.test(new Throwable())).isFalse();
        then(predicate.test(new Exception())).isFalse();
        then(predicate.test(new IOException())).isTrue();

    }

    @Test
    void buildComplexRecordExceptionsPredicateWithoutClasses() {
        Predicate<Throwable> exceptionPredicate = t -> t instanceof IOException || t instanceof RuntimeException;

        Predicate<Throwable> predicate = PredicateCreator
            .createExceptionsPredicate(exceptionPredicate)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isTrue();
        then(predicate.test(new IllegalArgumentException())).isTrue();
        then(predicate.test(new Throwable())).isFalse();
        then(predicate.test(new Exception())).isFalse();
        then(predicate.test(new IOException())).isTrue();

    }

    @Test
    void buildComplexRecordExceptionsPredicate() {
        Predicate<Throwable> exceptionPredicate = t -> t instanceof IOException;

        Predicate<Throwable> predicate = PredicateCreator
            .createExceptionsPredicate(exceptionPredicate, RuntimeException.class)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isTrue();
        then(predicate.test(new IllegalArgumentException())).isTrue();
        then(predicate.test(new Throwable())).isFalse();
        then(predicate.test(new Exception())).isFalse();
        then(predicate.test(new IOException())).isTrue();

    }

    @Test
    void buildRecordExceptionsPredicate() {
        Predicate<Throwable> predicate = PredicateCreator
            .createExceptionsPredicate(RuntimeException.class, IOException.class)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isTrue();
        then(predicate.test(new IllegalArgumentException())).isTrue();
        then(predicate.test(new Throwable())).isFalse();
        then(predicate.test(new Exception())).isFalse();
        then(predicate.test(new IOException())).isTrue();

    }

    @Test
    void buildIgnoreExceptionsPredicate() {
        Predicate<Throwable> predicate = PredicateCreator
            .createNegatedExceptionsPredicate(RuntimeException.class, BusinessException.class)
            .orElseThrow();

        then(predicate.test(new RuntimeException())).isFalse();
        then(predicate.test(new IllegalArgumentException())).isFalse();
        then(predicate.test(new Throwable())).isTrue();
        then(predicate.test(new Exception())).isTrue();
        then(predicate.test(new IOException())).isTrue();
        then(predicate.test(new BusinessException())).isFalse();
    }

    private class BusinessException extends Exception {

    }
}
