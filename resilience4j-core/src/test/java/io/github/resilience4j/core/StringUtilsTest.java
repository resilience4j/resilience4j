package io.github.resilience4j.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void string() {
        assertThat(StringUtils.isNotEmpty("bla")).isTrue();
    }

    @Test
    void emptyString() {
        assertThat(StringUtils.isNotEmpty("")).isFalse();
    }

    @Test
    void testNull() {
        assertThat(StringUtils.isNotEmpty(null)).isFalse();
    }
}
