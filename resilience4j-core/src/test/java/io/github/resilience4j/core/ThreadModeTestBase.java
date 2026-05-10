package io.github.resilience4j.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.stream.Stream;

/**
 * Base class providing thread-mode lifecycle and helper utilities.
 *
 * <p><strong>Preferred usage</strong>: annotate the test class with
 * {@code @ExtendWith(ThreadModeExtension.class)} and use {@code @TestTemplate} instead of
 * extending this class. The extension handles property save/restore automatically and injects
 * the {@link ThreadType} parameter with no boilerplate.
 *
 * <p>This base class is retained for cases where inheritance is needed (e.g. shared abstract
 * test hierarchies). When used directly, call {@link #setUpThreadMode(ThreadType)} at the
 * start of each parameterized test; the {@code @BeforeEach}/{@code @AfterEach} methods handle
 * save and restore of the original property value automatically.
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
