package io.github.resilience4j.common.bulkhead.configuration;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.common.CustomizerWithName;

/**
 * Enable customization bulkhead configuration builders programmatically.
 */
public interface BulkheadConfigCustomizer extends CustomizerWithName {

    /**
     * Customize BulkheadConfig configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(BulkheadConfig.Builder configBuilder);

}
