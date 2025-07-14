package io.github.resilience4j.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for the unified ThreadModeTestBase class.
 * 
 * This test was written using TDD approach to ensure all functionality
 * of the consolidated ThreadModeTestBase is properly verified.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
public class ThreadModeTestBaseTest {

    private String originalThreadType;
    
    @Before
    public void setUp() {
        originalThreadType = System.getProperty("resilience4j.thread.type");
    }
    
    @After
    public void tearDown() {
        if (originalThreadType != null) {
            System.setProperty("resilience4j.thread.type", originalThreadType);
        } else {
            System.clearProperty("resilience4j.thread.type");
        }
    }

    @Test
    public void shouldReturnPlatformAndVirtualThreadModes() {
        // When
        Collection<Object[]> threadModes = ThreadModeTestBase.threadModes();
        
        // Then
        assertThat(threadModes).hasSize(2);
        assertThat(threadModes).containsExactlyInAnyOrder(
            new Object[]{ThreadType.PLATFORM},
            new Object[]{ThreadType.VIRTUAL}
        );
    }

    @Test
    public void shouldSetUpVirtualThreadModeCorrectly() {
        // Given
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase(ThreadType.VIRTUAL);
        
        // When
        testBase.setUpThreadMode();
        
        // Then
        assertThat(System.getProperty("resilience4j.thread.type")).isEqualTo(ThreadType.VIRTUAL.toString());
    }

    @Test
    public void shouldSetUpPlatformThreadModeCorrectly() {
        // Given
        System.setProperty("resilience4j.thread.type", "someValue");
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase(ThreadType.PLATFORM);
        
        // When
        testBase.setUpThreadMode();
        
        // Then
        assertThat(System.getProperty("resilience4j.thread.type")).isNull();
    }

    @Test
    public void shouldClearSystemPropertyWhenOriginalWasNull() {
        // Given
        System.clearProperty("resilience4j.thread.type");
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase(ThreadType.VIRTUAL);
        testBase.setUpThreadMode();
        
        // When
        testBase.cleanUpThreadMode();
        
        // Then
        assertThat(System.getProperty("resilience4j.thread.type")).isNull();
    }

    @Test
    public void shouldReturnTrueForVirtualThreadMode() {
        // Given
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase(ThreadType.VIRTUAL);
        
        // When & Then
        assertThat(testBase.isVirtualThreadMode()).isTrue();
    }

    @Test
    public void shouldReturnFalseForPlatformThreadMode() {
        // Given
        TestableThreadModeTestBase testBase = new TestableThreadModeTestBase(ThreadType.PLATFORM);
        
        // When & Then
        assertThat(testBase.isVirtualThreadMode()).isFalse();
    }

    @Test
    public void shouldProvideCorrectThreadModeDescription() {
        // Given
        TestableThreadModeTestBase virtualTest = new TestableThreadModeTestBase(ThreadType.VIRTUAL);
        TestableThreadModeTestBase platformTest = new TestableThreadModeTestBase(ThreadType.PLATFORM);
        
        // When & Then
        assertThat(virtualTest.getThreadModeDescription()).isEqualTo("Virtual Thread Mode");
        assertThat(platformTest.getThreadModeDescription()).isEqualTo("Platform Thread Mode");
    }

    /**
     * Concrete implementation of ThreadModeTestBase for testing purposes.
     * Exposes protected methods for verification.
     */
    private static class TestableThreadModeTestBase extends ThreadModeTestBase {
        
        public TestableThreadModeTestBase(ThreadType threadType) {
            super(threadType);
        }
        
        public void setUpThreadMode() {
            super.setUpThreadMode();
        }
        
        public void cleanUpThreadMode() {
            super.cleanUpThreadMode();
        }
        
        public boolean isVirtualThreadMode() {
            return super.isVirtualThreadMode();
        }
        
        public String getThreadModeDescription() {
            return super.getThreadModeDescription();
        }
    }

    /**
     * Tests that ThreadModeTestBase supports JUnit Parameterized integration.
     */
    @Test
    public void shouldSupportParameterizedIntegration() {
        // Simple test to verify that classes extending ThreadModeTestBase work with Parameterized tests
        TestableThreadModeTestBase virtualTest = new TestableThreadModeTestBase(ThreadType.VIRTUAL);
        TestableThreadModeTestBase platformTest = new TestableThreadModeTestBase(ThreadType.PLATFORM);
        
        assertThat(ThreadModeTestBase.threadModes()).hasSize(2);
        assertThat(virtualTest.isVirtualThreadMode()).isTrue();
        assertThat(platformTest.isVirtualThreadMode()).isFalse();
    }
}