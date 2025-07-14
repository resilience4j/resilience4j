package io.github.resilience4j.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit Parameterized integration test.
 * 
 * Verifies that ThreadModeTestBase works correctly with @RunWith(Parameterized.class)
 * and runs tests for both platform and virtual thread modes.
 */
@RunWith(Parameterized.class)
public class ThreadModeTestBaseParameterizedIntegrationTest extends ThreadModeTestBase {
    
    public ThreadModeTestBaseParameterizedIntegrationTest(ThreadType threadType) {
        super(threadType);
    }
    
    @Test
    public void shouldRunWithBothThreadModes() {
        // This test runs twice: once for platform and once for virtual thread mode
        if (threadType == ThreadType.PLATFORM) {
            assertThat(System.getProperty("resilience4j.thread.type")).isNull();
            assertThat(isVirtualThreadMode()).isFalse();
            assertThat(getThreadModeDescription()).isEqualTo("Platform Thread Mode");
        } else if (threadType == ThreadType.VIRTUAL) {
            assertThat(System.getProperty("resilience4j.thread.type")).isEqualTo(ThreadType.VIRTUAL.toString());
            assertThat(isVirtualThreadMode()).isTrue();
            assertThat(getThreadModeDescription()).isEqualTo("Virtual Thread Mode");
        }
        
        // Common verification
        assertThat(getThreadModeDescription()).contains("Thread Mode");
    }
}