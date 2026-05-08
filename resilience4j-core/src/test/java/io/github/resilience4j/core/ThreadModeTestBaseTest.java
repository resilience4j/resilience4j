package io.github.resilience4j.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for the unified ThreadModeTestBase class.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
class ThreadModeTestBaseTest {

    private String originalThreadType;

    @BeforeEach
    void setUp() {
        originalThreadType = System.getProperty("resilience4j.thread.type");
    }

    @AfterEach
    void tearDown() {
        if (originalThreadType != null) {
            System.setProperty("resilience4j.thread.type", originalThreadType);
        } else {
            System.clearProperty("resilience4j.thread.type");
        }
    }

    @Test
    void shouldReturnPlatformAndVirtualThreadModes() {
        assertThat(ThreadModeTestBase.threadModes())
            .containsExactlyInAnyOrder(ThreadType.PLATFORM, ThreadType.VIRTUAL);
    }

    @Test
    void shouldSetUpVirtualThreadModeCorrectly() {
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase();
        testBase.setUpThreadMode(ThreadType.VIRTUAL);
        assertThat(System.getProperty("resilience4j.thread.type")).isEqualTo(ThreadType.VIRTUAL.toString());
    }

    @Test
    void shouldSetUpPlatformThreadModeCorrectly() {
        System.setProperty("resilience4j.thread.type", "someValue");
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase();
        testBase.setUpThreadMode(ThreadType.PLATFORM);
        assertThat(System.getProperty("resilience4j.thread.type")).isNull();
    }

    @Test
    void shouldRestoreSystemPropertyAfterTeardown() {
        System.clearProperty("resilience4j.thread.type");
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase();
        testBase.saveThreadModeProperty();
        testBase.setUpThreadMode(ThreadType.VIRTUAL);
        testBase.restoreThreadModeProperty();
        assertThat(System.getProperty("resilience4j.thread.type")).isNull();
    }

    @Test
    void shouldReturnTrueForVirtualThreadMode() {
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase();
        assertThat(testBase.isVirtualThreadMode(ThreadType.VIRTUAL)).isTrue();
    }

    @Test
    void shouldReturnFalseForPlatformThreadMode() {
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase();
        assertThat(testBase.isVirtualThreadMode(ThreadType.PLATFORM)).isFalse();
    }

    @Test
    void shouldProvideCorrectThreadModeDescription() {
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase();
        assertThat(testBase.getThreadModeDescription(ThreadType.VIRTUAL)).isEqualTo("Virtual Thread Mode");
        assertThat(testBase.getThreadModeDescription(ThreadType.PLATFORM)).isEqualTo("Platform Thread Mode");
    }

    @Test
    void shouldSupportParameterizedIntegration() {
        assertThat(ThreadModeTestBase.threadModes()).hasSize(2);
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase();
        assertThat(testBase.isVirtualThreadMode(ThreadType.VIRTUAL)).isTrue();
        assertThat(testBase.isVirtualThreadMode(ThreadType.PLATFORM)).isFalse();
    }

    private static class TestableThreadModeTestBase extends ThreadModeTestBase {
    }
}
