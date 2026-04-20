/*
 *   Copyright 2026: Deepak Kumar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.github.resilience4j.commons.configuration.util;

import io.github.resilience4j.commons.configuration.dummy.DummyPredicateThrowable;
import io.github.resilience4j.commons.configuration.exception.ConfigParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Predicate;

class ClassParseUtilTest {
    @Test
    void convertStringsToClassesList() {
        List<String> recordExceptionsList = List.of("java.lang.Exception", "java.lang.RuntimeException");

        Class<? extends Throwable>[] recordExceptions = ClassParseUtil
                .convertStringListToClassTypeArray(recordExceptionsList, Throwable.class);

        assertThat(recordExceptions).containsExactlyInAnyOrder(Exception.class, RuntimeException.class);
    }

    @Test
    void convertStringToClass() {
        Class<? extends Throwable> clazz = ClassParseUtil.convertStringToClassType("java.lang.Exception", Throwable.class);

        assertThat(clazz).isEqualTo(Exception.class);
    }

    @Test
    void convertPredicate() {
        String predicateString = "io.github.resilience4j.commons.configuration.dummy.DummyPredicateThrowable";

        Class<Predicate<Throwable>> clazz = (Class<Predicate<Throwable>>) ClassParseUtil.convertStringToClassType(predicateString, Predicate.class);

        assertThat(clazz).isEqualTo(DummyPredicateThrowable.class);
    }

    @Test
    void convertPredicateInvalidType() {
        assertThatThrownBy(() -> ClassParseUtil.convertStringToClassType("java.lang.Exception", Predicate.class))
            .isInstanceOf(ConfigParseException.class);
    }
}