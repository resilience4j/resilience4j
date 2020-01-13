package io.github.resilience4j.common.bulkhead.configuration;

import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.common.CustomizerWithName;

/**
 * Enable customization thread pool bulkhead configuration builders programmatically.
 */
public interface ThreadPoolBulkheadConfigCustomizer extends CustomizerWithName {

    /**
     * Customize ThreadPoolBulkheadConfig configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(ThreadPoolBulkheadConfig.Builder configBuilder);

}
