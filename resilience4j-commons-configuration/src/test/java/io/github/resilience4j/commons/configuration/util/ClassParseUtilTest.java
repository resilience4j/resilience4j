/*
 *   Copyright 2023: Deepak Kumar
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
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;
import java.util.function.Predicate;

public class ClassParseUtilTest {
    @Test
    public void testConvertStringsToClassesList() {
        List<String> recordExceptionsList = List.of("java.lang.Exception", "java.lang.RuntimeException");

        Class<? extends Throwable>[] recordExceptions = ClassParseUtil
                .convertStringListToClassTypeArray(recordExceptionsList, Throwable.class);

        Assertions.assertThat(recordExceptions).containsExactlyInAnyOrder(Exception.class, RuntimeException.class);
    }

    @Test
    public void testConvertStringToClass() {
        Class<? extends Throwable> clazz = ClassParseUtil.convertStringToClassType("java.lang.Exception", Throwable.class);

        Assertions.assertThat(clazz).isEqualTo(Exception.class);
    }

    @Test
    public void testConvertPredicate() {
        String predicateString = "io.github.resilience4j.commons.configuration.dummy.DummyPredicateThrowable";

        Class<Predicate<Throwable>> clazz = (Class<Predicate<Throwable>>) ClassParseUtil.convertStringToClassType(predicateString, Predicate.class);

        Assertions.assertThat(clazz).isEqualTo(DummyPredicateThrowable.class);
    }

    @Test(expected = ConfigParseException.class)
    public void testConvertPredicateInvalidType() {
        String predicateString = "java.lang.Exception";

        ClassParseUtil.convertStringToClassType(predicateString, Predicate.class);
    }
}