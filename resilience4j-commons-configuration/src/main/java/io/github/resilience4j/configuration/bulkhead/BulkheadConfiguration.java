package io.github.resilience4j.configuration.bulkhead;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.configuration.InheritedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;

/**
 * {@link BulkheadConfiguration} is used to create {@link BulkheadConfig} from a {@link Configuration}.
 * <p>
 * Each instance of BulkheadConfiguration is used to access properties for a specific named {@link Configuration#subset(String) subset} from a specific
 * contextual subset of the Configuration. The {@code default contextual subset} is accessed by the prefix
 * {@code io.github.resilience4j.bulkhead}, and the {@code default named subset} prefix is {@code default}.
 * </p>
 * <p>
 * A BulkheadConfiguration may access properties for one or more named subsets within the contextual subset.
 * </p>
 */
@Immutable
public class BulkheadConfiguration extends InheritedConfiguration<BulkheadConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(BulkheadConfiguration.class);
    private static final String DEFAULT_CONTEXT = "io.github.resilience4j.bulkhead";

    /**
     * Initializes a {@link BulkheadConfiguration} using the provided {@code config} and the
     * {@code default contextual subset}.
     *
     * @param config the Configuration.
     * @see #BulkheadConfiguration(Configuration, String)
     */
    public BulkheadConfiguration(final Configuration config) {
        this(config, DEFAULT_CONTEXT);
    }

    /**
     * Initializes a {@link BulkheadConfiguration} using the provided {@code config} and {@code context}
     * {@link Configuration#subset(String) subset}.
     *
     * @param config  the Configuration.
     * @param context the contextual prefix from which named bulkhead configuration will be accessed.
     */
    public BulkheadConfiguration(final Configuration config, final String context) {
        super(config, context);
    }

    @Override
    protected BulkheadConfig map(Configuration config, BulkheadConfig defaults) {
        return BulkheadConfig.custom()
            .maxConcurrentCalls(config.getInt("maxConcurrentCalls", defaults.getMaxConcurrentCalls()))
            .maxWaitDuration(getDuration(config, "maxWaitDuration", defaults.getMaxWaitDuration()))
            .writableStackTraceEnabled(config.getBoolean("writableStackTraceEnabled", defaults.isWritableStackTraceEnabled())).build();
    }

    @Override
    protected BulkheadConfig getDefaultConfigObject() {
        return BulkheadConfig.ofDefaults();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
