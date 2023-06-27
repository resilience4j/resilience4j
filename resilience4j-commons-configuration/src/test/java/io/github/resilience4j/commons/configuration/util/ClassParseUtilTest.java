package io.github.resilience4j.commons.configuration.util;

import io.github.resilience4j.commons.configuration.dummy.DummyRecordFailurePredicate;
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
        String predicateString = "io.github.resilience4j.commons.configuration.dummy.DummyRecordFailurePredicate";

        Class<Predicate<Throwable>> clazz = (Class<Predicate<Throwable>>) ClassParseUtil.convertStringToClassType(predicateString, Predicate.class);

        Assertions.assertThat(clazz).isEqualTo(DummyRecordFailurePredicate.class);
    }

    @Test(expected = ConfigParseException.class)
    public void testConvertPredicateInvalidType() {
        String predicateString = "java.lang.Exception";

        ClassParseUtil.convertStringToClassType(predicateString, Predicate.class);
    }
}