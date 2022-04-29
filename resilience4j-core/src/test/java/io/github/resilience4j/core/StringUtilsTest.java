package io.github.resilience4j.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTest {

    @Test
    public void testString() {
        assertThat(StringUtils.isNotEmpty("bla")).isTrue();
    }

    @Test
    public void testEmptyString() {
        assertThat(StringUtils.isNotEmpty("")).isFalse();
    }

    @Test
    public void testNull() {
        assertThat(StringUtils.isNotEmpty(null)).isFalse();
    }
}
