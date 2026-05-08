package io.github.resilience4j.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.stream.Stream;

/**
 * Base class for JUnit 5 parameterized tests that run in both platform and virtual thread modes.
 *
 * <p>Subclasses use {@code @ParameterizedTest @MethodSource("threadModes")} (or
 * {@code @EnumSource(ThreadType.class)}) and accept a {@link ThreadType} parameter.
 * Before each test, call {@link #setUpThreadMode(ThreadType)} to configure the system
 * property; the {@code @AfterEach} teardown restores the original value automatically.
 *
 * <p>Example usage:
 * <pre>{@code
 * class MyTest extends ThreadModeTestBase {
 *
 *     @ParameterizedTest(name = "{0} thread mode")
 *     @EnumSource(ThreadType.class)
 *     void myTest(ThreadType threadType) throws Exception {
 *         setUpThreadMode(threadType);
 *         // ... test body using isVirtualThreadMode(threadType) etc.
 *     }
 * }
 * }</pre>
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
public abstract class ThreadModeTestBase {

    protected static final String SYS_PROP_KEY = "resilience4j.thread.type";

    private String originalPropertyValue;

    @BeforeEach
    public void saveThreadModeProperty() {
        originalPropertyValue = System.getProperty(SYS_PROP_KEY);
    }

    @AfterEach
    public void restoreThreadModeProperty() {
        if (originalPropertyValue != null) {
            System.setProperty(SYS_PROP_KEY, originalPropertyValue);
        } else {
            System.clearProperty(SYS_PROP_KEY);
        }
    }

    /**
     * Configure the system property for the given thread mode.
     * Call this at the start of each parameterized test method.
     */
    protected void setUpThreadMode(ThreadType threadType) {
        if (threadType == ThreadType.VIRTUAL) {
            System.setProperty(SYS_PROP_KEY, threadType.toString());
        } else {
            System.clearProperty(SYS_PROP_KEY);
        }
    }

    /**
     * Returns a stream of all {@link ThreadType} values for use as a {@code @MethodSource}.
     */
    public static Stream<ThreadType> threadModes() {
        return Stream.of(ThreadType.values());
    }

    /** Returns true if running in virtual thread mode. */
    protected boolean isVirtualThreadMode(ThreadType threadType) {
        return threadType == ThreadType.VIRTUAL;
    }

    /** Returns a descriptive string for the given thread mode. */
    protected String getThreadModeDescription(ThreadType threadType) {
        return isVirtualThreadMode(threadType) ? "Virtual Thread Mode" : "Platform Thread Mode";
    }
}
