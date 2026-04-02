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

    public static GenericBulkheadConfig ofDefaults() {
    return new GenericBulkheadConfig();
    }

    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled;
    }
}
