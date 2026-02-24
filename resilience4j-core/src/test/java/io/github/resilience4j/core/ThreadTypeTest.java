package io.github.resilience4j.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ThreadType} enum.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
public class ThreadTypeTest {

    @Test
    public void shouldHaveVirtualType() {
        assertThat(ThreadType.VIRTUAL).isNotNull();
        assertThat(ThreadType.VIRTUAL.toString()).isEqualTo("virtual");
    }

    @Test
    public void shouldHavePlatformType() {
        assertThat(ThreadType.PLATFORM).isNotNull();
        assertThat(ThreadType.PLATFORM.toString()).isEqualTo("platform");
    }

    @Test
    public void shouldReturnPlatformAsDefault() {
        assertThat(ThreadType.getDefault()).isEqualTo(ThreadType.PLATFORM);
    }

    @Test
    public void shouldParseVirtualFromString() {
        assertThat(ThreadType.fromString("virtual")).isEqualTo(ThreadType.VIRTUAL);
        assertThat(ThreadType.fromString("VIRTUAL")).isEqualTo(ThreadType.VIRTUAL);
        assertThat(ThreadType.fromString("Virtual")).isEqualTo(ThreadType.VIRTUAL);
    }

    @Test
    public void shouldParsePlatformFromString() {
        assertThat(ThreadType.fromString("platform")).isEqualTo(ThreadType.PLATFORM);
        assertThat(ThreadType.fromString("PLATFORM")).isEqualTo(ThreadType.PLATFORM);
        assertThat(ThreadType.fromString("Platform")).isEqualTo(ThreadType.PLATFORM);
    }

    @Test
    public void shouldReturnDefaultForNullInput() {
        assertThat(ThreadType.fromString(null)).isEqualTo(ThreadType.PLATFORM);
    }

    @Test
    public void shouldReturnDefaultForEmptyInput() {
        assertThat(ThreadType.fromString("")).isEqualTo(ThreadType.PLATFORM);
        assertThat(ThreadType.fromString("   ")).isEqualTo(ThreadType.PLATFORM);
    }

    @Test
    public void shouldThrowExceptionForInvalidInput() {
        assertThatThrownBy(() -> ThreadType.fromString("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown thread type: invalid");
        
        assertThatThrownBy(() -> ThreadType.fromString("other"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown thread type: other");
    }

    @Test
    public void shouldReturnCorrectStringRepresentation() {
        assertThat(ThreadType.VIRTUAL.toString()).isEqualTo("virtual");
        assertThat(ThreadType.PLATFORM.toString()).isEqualTo("platform");
    }

    @Test
    public void shouldSupportSafeParsingWithDefault() {
        assertThat(ThreadType.fromStringOrDefault("virtual", ThreadType.PLATFORM))
            .isEqualTo(ThreadType.VIRTUAL);

        assertThat(ThreadType.fromStringOrDefault("invalid", ThreadType.VIRTUAL))
            .isEqualTo(ThreadType.VIRTUAL);

        assertThat(ThreadType.fromStringOrDefault(null, ThreadType.VIRTUAL))
            .isEqualTo(ThreadType.VIRTUAL);
    }
}