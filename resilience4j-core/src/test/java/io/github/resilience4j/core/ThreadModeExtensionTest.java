package io.github.resilience4j.core;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that {@link ThreadModeExtension} correctly runs each
 * {@code @TestTemplate} method for both platform and virtual thread modes.
 */
@ExtendWith(ThreadModeExtension.class)
class ThreadModeExtensionTest {

    @TestTemplate
    void shouldRunWithBothThreadModes(ThreadType threadType) {
        if (threadType == ThreadType.PLATFORM) {
            assertThat(System.getProperty("resilience4j.thread.type")).isNull();
        } else if (threadType == ThreadType.VIRTUAL) {
            assertThat(System.getProperty("resilience4j.thread.type"))
                .isEqualTo(ThreadType.VIRTUAL.toString());
        }
    }
}
