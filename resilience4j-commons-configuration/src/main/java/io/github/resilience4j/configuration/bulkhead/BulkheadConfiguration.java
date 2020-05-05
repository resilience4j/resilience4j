package io.github.resilience4j.configuration.bulkhead;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.concurrent.Immutable;

import static io.github.resilience4j.configuration.utils.ConfigurationUtil.getDuration;

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
public class BulkheadConfiguration {
    private static final String DEFAULT_NAME = "default";
    private static final String DEFAULT_CONTEXT = "io.github.resilience4j.bulkhead";
    private final Configuration config;

    /**
     * Initializes a {@link BulkheadConfiguration} using the provided {@code config} and {@code context}
     * {@link Configuration#subset(String) subset}.
     *
     * @param config  the Configuration.
     * @param context the contextual prefix from which named bulkhead configuration will be accessed.
     */
    public BulkheadConfiguration(final Configuration config, final String context) {
        this.config = config.subset(context);
    }

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
     * Creates a {@link BulkheadConfig} with the properties accessed at the {@link Configuration#subset(String) subset}
     * with the provided {@code name}. If a property is not accessible in the Configuration, the value will be
     * replicated from the provided {@code defaultConfig}.
     *
     * @param name          the subset prefix of the contextual Configuration subset from which to create the
     *                      BulkheadConfig.
     * @param defaultConfig the BulkheadConfig used to provide values for unconfigured properties.
     * @return a BulkheadConfig.
     */
    public BulkheadConfig get(final String name, final BulkheadConfig defaultConfig) {
        final Configuration namedConfig = config.subset(name);

        return BulkheadConfig.custom()
            .maxConcurrentCalls(
                namedConfig.getInt("maxConcurrentCalls", defaultConfig.getMaxConcurrentCalls()))
            .maxWaitDuration(getDuration(namedConfig, "maxWaitTime", defaultConfig.getMaxWaitDuration()))
            .writableStackTraceEnabled(
                namedConfig.getBoolean("writableStackTraceEnabled", defaultConfig.isWritableStackTraceEnabled()))
            .build();
    }

    /**
     * Creates a {@link BulkheadConfig} with the properties accessed at the {@code default named subset}. If a
     * property is not accessible in the Configuration, the value will be replicated from the
     * {@link BulkheadConfig#ofDefaults() default BulkheadConfig}.
     *
     * @return the default bulkhead configuration
     * @see #get(String, BulkheadConfig)
     * @see BulkheadConfig#ofDefaults()
     */
    public BulkheadConfig getDefault() {
        return get(DEFAULT_NAME, BulkheadConfig.ofDefaults());
    }

    /**
     * Creates a {@link BulkheadConfig} with the properties accessed at the {@link Configuration#subset(String) subset}
     * with the provided {@code name}. If a property is not accessible in the Configuration, the value will be
     * replicated from the {@link #getDefault() configured default}.
     *
     * @param name the subset prefix of the Configuration from which to create the BulkheadConfig.
     * @return a BulkheadConfig
     * @see BulkheadConfiguration#get(String, BulkheadConfig)
     * @see BulkheadConfiguration#getDefault()
     */
    public BulkheadConfig get(final String name) {
        return get(name, getDefault());
    }
}
