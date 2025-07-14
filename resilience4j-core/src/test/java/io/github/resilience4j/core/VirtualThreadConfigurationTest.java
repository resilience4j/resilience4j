package io.github.resilience4j.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

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
public class VirtualThreadConfigurationTest {

    private static final String SYS_PROP_KEY = "resilience4j.thread.type";
    private static final String ENV_VAR_KEY = "RESILIENCE4J_THREAD_TYPE";
    
    private String originalSysProperty;

    @Before
    public void setUp() {
        // Store original values for restoration
        originalSysProperty = System.getProperty(SYS_PROP_KEY);
        
        // Clear any existing configuration
        System.clearProperty(SYS_PROP_KEY);
    }

    @After
    public void tearDown() {
        // Restore original configuration
        if (originalSysProperty != null) {
            System.setProperty(SYS_PROP_KEY, originalSysProperty);
        } else {
            System.clearProperty(SYS_PROP_KEY);
        }
    }

    @Test
    public void shouldUseVirtualThreadsWhenSystemPropertyIsVirtual() throws Exception {
        // Skip test if not running on Java 21+
        assumeTrue("Virtual threads require Java 21+", isJava21OrLater());
        System.setProperty(SYS_PROP_KEY, ThreadType.VIRTUAL.toString());
        
        assertTrue(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
        
        ExecutorService executor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("test");
        Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());
        
        try {
            assertTrue(isVirtual.get(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldUsePlatformThreadsWhenSystemPropertyIsPlatform() throws Exception {
        assumeTrue("Virtual threads require Java 21+", isJava21OrLater());
        System.setProperty(SYS_PROP_KEY, ThreadType.PLATFORM.toString());
        
        assertFalse(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
        
        ExecutorService executor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("test");
        Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());
        
        try {
            assertFalse(isVirtual.get(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldDefaultToPlatformThreadsForInvalidValues() throws Exception {
        assumeTrue("Virtual threads require Java 21+", isJava21OrLater());
        
        String[] invalidValues = {"invalid", "true", "false", "1", "0", "yes", "no", ""};
        
        for (String invalidValue : invalidValues) {
            System.setProperty(SYS_PROP_KEY, invalidValue);
            
            assertFalse("Invalid value '" + invalidValue + "' should default to platform threads", 
                       ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
            
            ExecutorService executor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("test");
            Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());
            
            try {
                assertFalse("Invalid value '" + invalidValue + "' should use platform threads", 
                           isVirtual.get(1, TimeUnit.SECONDS));
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void shouldBeCaseInsensitive() throws Exception {
        assumeTrue("Virtual threads require Java 21+", isJava21OrLater());
        System.setProperty(SYS_PROP_KEY, "VIRTUAL");
        assertTrue(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
        
        System.setProperty(SYS_PROP_KEY, "Virtual");
        assertTrue(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
        
        System.setProperty(SYS_PROP_KEY, "vIrTuAl");
        assertTrue(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
        
        System.setProperty(SYS_PROP_KEY, "PLATFORM");
        assertFalse(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
    }

    @Test
    public void shouldSwitchThreadTypeDynamically() throws Exception {
        assumeTrue("Virtual threads require Java 21+", isJava21OrLater());
        // Start with platform threads
        System.setProperty(SYS_PROP_KEY, ThreadType.PLATFORM.toString());
        assertFalse(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
        
        ExecutorService platformExecutor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("platform-test");
        Future<Boolean> platformResult = platformExecutor.submit(() -> Thread.currentThread().isVirtual());
        
        try {
            assertFalse(platformResult.get(1, TimeUnit.SECONDS));
        } finally {
            platformExecutor.shutdownNow();
        }
        
        // Switch to virtual threads
        System.setProperty(SYS_PROP_KEY, ThreadType.VIRTUAL.toString());
        assertTrue(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
        
        ExecutorService virtualExecutor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("virtual-test");
        Future<Boolean> virtualResult = virtualExecutor.submit(() -> Thread.currentThread().isVirtual());
        
        try {
            assertTrue(virtualResult.get(1, TimeUnit.SECONDS));
        } finally {
            virtualExecutor.shutdownNow();
        }
    }

    @Test
    public void shouldHandleNullAndEmptyValues() {
        assumeTrue("Virtual threads require Java 21+", isJava21OrLater());
        // Test null property (cleared)
        System.clearProperty(SYS_PROP_KEY);
        assertFalse(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
        
        // Test empty string
        System.setProperty(SYS_PROP_KEY, "");
        assertFalse(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
        
        // Test whitespace
        System.setProperty(SYS_PROP_KEY, "   ");
        assertFalse(ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL);
    }

    @Test
    public void shouldHandlePoolSizeConfiguration() throws Exception {
        assumeTrue("Virtual threads require Java 21+", isJava21OrLater());
        System.setProperty(SYS_PROP_KEY, ThreadType.VIRTUAL.toString());
        
        // Test single thread scheduler
        ExecutorService singleThreadExecutor = ExecutorServiceFactory.newSingleThreadScheduledExecutor("single");
        Future<Boolean> singleResult = singleThreadExecutor.submit(() -> Thread.currentThread().isVirtual());
        
        try {
            assertTrue(singleResult.get(1, TimeUnit.SECONDS));
        } finally {
            singleThreadExecutor.shutdownNow();
        }
        
        // Test multi-thread scheduler
        ExecutorService multiThreadExecutor = ExecutorServiceFactory.newScheduledThreadPool(5, "multi");
        Future<Boolean> multiResult = multiThreadExecutor.submit(() -> Thread.currentThread().isVirtual());
        
        try {
            assertTrue(multiResult.get(1, TimeUnit.SECONDS));
        } finally {
            multiThreadExecutor.shutdownNow();
        }
    }

    @Test
    public void shouldDocumentEnvironmentVariableSupport() {
        assumeTrue("Virtual threads require Java 21+", isJava21OrLater());
        
        // This test documents that RESILIENCE4J_THREAD_TYPE environment variable
        // is supported as fallback when system property is not set.
        // Note: We cannot test this directly in unit tests as environment variables
        // cannot be modified at runtime. This would require integration tests
        // with different process environments.
        
        String envValue = System.getenv(ENV_VAR_KEY);
        if (envValue != null) {
            // If environment variable is set, verify it follows the same rules
            assertTrue("Environment variable should be 'virtual' or 'platform'", 
                      ThreadType.VIRTUAL.toString().equalsIgnoreCase(envValue) || ThreadType.PLATFORM.toString().equalsIgnoreCase(envValue));
        }
        
        // Verify the constant is properly defined
        assertEquals("Environment variable key should match expected constant", 
                     "RESILIENCE4J_THREAD_TYPE", ENV_VAR_KEY);
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