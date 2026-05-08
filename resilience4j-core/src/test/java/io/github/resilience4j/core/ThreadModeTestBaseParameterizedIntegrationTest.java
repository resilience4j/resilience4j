package io.github.resilience4j.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 5 parameterized integration test.
 * <p>
 * Verifies that ThreadModeTestBase works correctly with {@code @ParameterizedTest}
 * and runs tests for both platform and virtual thread modes.
 */
class ThreadModeTestBaseParameterizedIntegrationTest extends ThreadModeTestBase {

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void shouldRunWithBothThreadModes(ThreadType threadType) {
        setUpThreadMode(threadType);

        if (threadType == ThreadType.PLATFORM) {
            assertThat(System.getProperty("resilience4j.thread.type")).isNull();
            assertThat(isVirtualThreadMode(threadType)).isFalse();
            assertThat(getThreadModeDescription(threadType)).isEqualTo("Platform Thread Mode");
        } else if (threadType == ThreadType.VIRTUAL) {
            assertThat(System.getProperty("resilience4j.thread.type")).isEqualTo(ThreadType.VIRTUAL.toString());
            assertThat(isVirtualThreadMode(threadType)).isTrue();
            assertThat(getThreadModeDescription(threadType)).isEqualTo("Virtual Thread Mode");
        }

        assertThat(getThreadModeDescription(threadType)).contains("Thread Mode");
    }
}
