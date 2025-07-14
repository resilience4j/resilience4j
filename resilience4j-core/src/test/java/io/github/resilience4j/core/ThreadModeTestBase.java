package io.github.resilience4j.core;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unified ThreadModeTestBase class for testing with both platform and virtual threads.
 * 
 * This class consolidates the previously duplicated ThreadModeTestBase implementations
 * from multiple modules into a single shared implementation in resilience4j-core.
 * It provides parameterized test support for running tests in both thread modes.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
public abstract class ThreadModeTestBase {

    protected static final String SYS_PROP_KEY = "resilience4j.thread.type";
    
    protected ThreadType threadType;
    private String originalPropertyValue;

    /**
     * Constructor for parameterized tests.
     * 
     * @param threadType the thread type to test with
     */
    public ThreadModeTestBase(ThreadType threadType) {
        this.threadType = threadType;
    }

    @Parameterized.Parameters(name = "threadMode={0}")
    public static Collection<Object[]> threadModes() {
        return Arrays.asList(new Object[][] {
            {ThreadType.PLATFORM}, // Default platform threads
            {ThreadType.VIRTUAL}   // Virtual threads
        });
    }

    @Before
    public void setUpThreadMode() {
        // Save original property value
        originalPropertyValue = System.getProperty(SYS_PROP_KEY);
        
        // Configure thread mode for test
        if (threadType == ThreadType.VIRTUAL) {
            // Virtual threads require explicit activation via system property
            // This sets "resilience4j.thread.type=virtual" to enable virtual thread mode
            System.setProperty(SYS_PROP_KEY, threadType.toString());
        } else {
            // Platform threads are the default when no property is set
            // Clear the property to ensure we fall back to platform thread mode
            // This prevents interference from previous test runs
            System.clearProperty(SYS_PROP_KEY);
        }
    }

    @After
    public void cleanUpThreadMode() {
        // Restore original property value
        if (originalPropertyValue != null) {
            System.setProperty(SYS_PROP_KEY, originalPropertyValue);
        } else {
            System.clearProperty(SYS_PROP_KEY);
        }
    }

    /**
     * Returns true if running in virtual thread mode.
     */
    protected boolean isVirtualThreadMode() {
        return threadType == ThreadType.VIRTUAL;
    }
    
    /**
     * Returns a descriptive string for the current thread mode.
     */
    protected String getThreadModeDescription() {
        return isVirtualThreadMode() ? "Virtual Thread Mode" : "Platform Thread Mode";
    }
}