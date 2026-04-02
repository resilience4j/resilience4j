package io.github.resilience4j.bulkhead;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;

/**
 * Base Bulkhead configuration.
 */
public class GenericBulkheadConfig implements Serializable {
    public static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;
    private static final long serialVersionUID = 625685690021881900L;

    protected boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;

    /**
     * Creates a default GenericBulkhead configuration.
     *
     * @return a default GenericBulkhead configuration.
     */
    public static GenericBulkheadConfig ofDefaults() {
        return new GenericBulkheadConfig();
    }

    /**
     * Checks if writable stack trace is enabled.
     *
     * @return true if enabled, false otherwise.
     */
    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled;
    }
}
