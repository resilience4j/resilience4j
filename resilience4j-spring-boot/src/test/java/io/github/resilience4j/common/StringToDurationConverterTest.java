package io.github.resilience4j.common;


import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * String to duration test
 */
public class StringToDurationConverterTest {

    @Test
    public void testStringToDurationConverter() {

        StringToDurationConverter stringToDurationConverter = new StringToDurationConverter();

        assertThat(stringToDurationConverter.convert("10s")).isEqualTo(Duration.ofSeconds(10));
        assertThat(stringToDurationConverter.convert("10ns")).isEqualTo(Duration.ofNanos(10));
        assertThat(stringToDurationConverter.convert("10m")).isEqualTo(Duration.ofMinutes(10));
        assertThat(stringToDurationConverter.convert("10ms")).isEqualTo(Duration.ofMillis(10));
        assertThat(stringToDurationConverter.convert("10d")).isEqualTo(Duration.ofDays(10));
        assertThat(stringToDurationConverter.convert("10h")).isEqualTo(Duration.ofHours(10));

        assertThat(stringToDurationConverter.convert(null)).isNull();
        assertThat(stringToDurationConverter.convert("BlaBla")).isNull();


    }


    @Test
    public void testIntegerToDuration() {
        IntegerToDurationConverter integerToDurationConverter = new IntegerToDurationConverter();
        assertThat(integerToDurationConverter.convert(10)).isEqualTo(Duration.ofMillis(10));
        assertThat(integerToDurationConverter.convert(null)).isNull();
    }

}