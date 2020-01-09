package io.github.resilience4j.common.retry.configuration;

import io.github.resilience4j.common.CustomizerWithName;
import io.github.resilience4j.retry.RetryConfig;

/**
 * Enable customization retry configuration builders programmatically.
 */
public interface RetryConfigCustomizer extends CustomizerWithName {

    /**
     * Retry configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(RetryConfig.Builder configBuilder);
}
