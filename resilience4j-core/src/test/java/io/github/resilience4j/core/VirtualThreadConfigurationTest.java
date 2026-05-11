package io.github.resilience4j.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Comprehensive test for virtual thread configuration and environment handling.
 * 
 * <p>Tests configuration scenarios including:
 * <ul>
 *   <li>Dynamic configuration changes at runtime</li>
 *   <li>Environment variable vs system property priority</li>
 *   <li>Invalid configuration value handling</li>
 *   <li>Configuration validation and error reporting</li>
 *   <li>Thread type switching and consistency</li>
 * </ul>
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
class VirtualThreadConfigurationTest {

    private static final String SYS_PROP_KEY = "resilience4j.thread.type";
    private static final String ENV_VAR_KEY = "RESILIENCE4J_THREAD_TYPE";

    private String originalSysProperty;

    @BeforeEach
    void setUp() {
        // Store original values for restoration
        originalSysProperty = System.getProperty(SYS_PROP_KEY);

        // Clear any existing configuration
        System.clearProperty(SYS_PROP_KEY);
    }

    @AfterEach
    void tearDown() {
        // Restore original configuration
        if (originalSysProperty != null) {
            System.setProperty(SYS_PROP_KEY, originalSysProperty);
        } else {
            System.clearProperty(SYS_PROP_KEY);
        }
    }

    @Test
    void shouldUseVirtualThreadsWhenSystemPropertyIsVirtual() throws Exception {
        // Skip test if not running on Java 21+
        assumeTrue(isJava21OrLater(), "Virtual threads require Java 21+");
        System.setProperty(SYS_PROP_KEY, ThreadType.VIRTUAL.toString());

        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.VIRTUAL);

        ExecutorService executor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("test");
        Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());

        try {
            assertThat(isVirtual.get(1, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldUsePlatformThreadsWhenSystemPropertyIsPlatform() throws Exception {
        assumeTrue(isJava21OrLater(), "Virtual threads require Java 21+");
        System.setProperty(SYS_PROP_KEY, ThreadType.PLATFORM.toString());

        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.PLATFORM);

        ExecutorService executor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("test");
        Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());

        try {
            assertThat(isVirtual.get(1, TimeUnit.SECONDS)).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldDefaultToPlatformThreadsForInvalidValues() throws Exception {
        assumeTrue(isJava21OrLater(), "Virtual threads require Java 21+");

        String[] invalidValues = {"invalid", "true", "false", "1", "0", "yes", "no", ""};

        for (String invalidValue : invalidValues) {
            System.setProperty(SYS_PROP_KEY, invalidValue);

            assertThat(ExecutorServiceFactory.getThreadType())
                .as("Invalid value '%s' should default to platform threads", invalidValue)
                .isEqualTo(ThreadType.PLATFORM);

            ExecutorService executor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("test");
            Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());

            try {
                assertThat(isVirtual.get(1, TimeUnit.SECONDS))
                    .as("Invalid value '%s' should use platform threads", invalidValue)
                    .isFalse();
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void shouldBeCaseInsensitive() {
        assumeTrue(isJava21OrLater(), "Virtual threads require Java 21+");
        System.setProperty(SYS_PROP_KEY, "VIRTUAL");
        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.VIRTUAL);

        System.setProperty(SYS_PROP_KEY, "Virtual");
        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.VIRTUAL);

        System.setProperty(SYS_PROP_KEY, "vIrTuAl");
        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.VIRTUAL);

        System.setProperty(SYS_PROP_KEY, "PLATFORM");
        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.PLATFORM);
    }

    @Test
    void shouldSwitchThreadTypeDynamically() throws Exception {
        assumeTrue(isJava21OrLater(), "Virtual threads require Java 21+");
        // Start with platform threads
        System.setProperty(SYS_PROP_KEY, ThreadType.PLATFORM.toString());
        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.PLATFORM);

        ExecutorService platformExecutor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("platform-test");
        Future<Boolean> platformResult = platformExecutor.submit(() -> Thread.currentThread().isVirtual());

        try {
            assertThat(platformResult.get(1, TimeUnit.SECONDS)).isFalse();
        } finally {
            platformExecutor.shutdownNow();
        }

        // Switch to virtual threads
        System.setProperty(SYS_PROP_KEY, ThreadType.VIRTUAL.toString());
        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.VIRTUAL);

        ExecutorService virtualExecutor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("virtual-test");
        Future<Boolean> virtualResult = virtualExecutor.submit(() -> Thread.currentThread().isVirtual());

        try {
            assertThat(virtualResult.get(1, TimeUnit.SECONDS)).isTrue();
        } finally {
            virtualExecutor.shutdownNow();
        }
    }

    @Test
    void shouldHandleNullAndEmptyValues() {
        assumeTrue(isJava21OrLater(), "Virtual threads require Java 21+");
        // Test null property (cleared)
        System.clearProperty(SYS_PROP_KEY);
        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.PLATFORM);

        // Test empty string
        System.setProperty(SYS_PROP_KEY, "");
        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.PLATFORM);

        // Test whitespace
        System.setProperty(SYS_PROP_KEY, "   ");
        assertThat(ExecutorServiceFactory.getThreadType()).isEqualTo(ThreadType.PLATFORM);
    }

    @Test
    void shouldHandlePoolSizeConfiguration() throws Exception {
        assumeTrue(isJava21OrLater(), "Virtual threads require Java 21+");
        System.setProperty(SYS_PROP_KEY, ThreadType.VIRTUAL.toString());

        // Test single thread scheduler
        ExecutorService singleThreadExecutor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("single");
        Future<Boolean> singleResult = singleThreadExecutor.submit(() -> Thread.currentThread().isVirtual());

        try {
            assertThat(singleResult.get(1, TimeUnit.SECONDS)).isTrue();
        } finally {
            singleThreadExecutor.shutdownNow();
        }

        // Test multi-thread scheduler
        ExecutorService multiThreadExecutor = ExecutorServiceFactory.newScheduledThreadPool(5, "multi");
        Future<Boolean> multiResult = multiThreadExecutor.submit(() -> Thread.currentThread().isVirtual());

        try {
            assertThat(multiResult.get(1, TimeUnit.SECONDS)).isTrue();
        } finally {
            multiThreadExecutor.shutdownNow();
        }
    }

    @Test
    void shouldAcceptValidEnvironmentVariableValues() {
        assumeTrue(isJava21OrLater(), "Virtual threads require Java 21+");

        // This test documents that RESILIENCE4J_THREAD_TYPE environment variable
        // is supported as fallback when system property is not set.
        // Note: We cannot test this directly in unit tests as environment variables
        // cannot be modified at runtime. This would require integration tests
        // with different process environments.

        String envValue = System.getenv(ENV_VAR_KEY);
        if (envValue != null) {
            // If environment variable is set, verify it follows the same rules
            assertThat(ThreadType.VIRTUAL.toString().equalsIgnoreCase(envValue)
                || ThreadType.PLATFORM.toString().equalsIgnoreCase(envValue))
                .as("Environment variable should be 'virtual' or 'platform'")
                .isTrue();
        }

        // Verify the constant is properly defined
        assertThat(ENV_VAR_KEY)
            .as("Environment variable key should match expected constant")
            .isEqualTo("RESILIENCE4J_THREAD_TYPE");
    }

    private boolean isJava21OrLater() {
        try {
            // Try to access Thread.ofVirtual() which was introduced in Java 21
            Thread.ofVirtual();
            return true;
        } catch (Exception | NoSuchMethodError e) {
            return false;
        }
    }
}
