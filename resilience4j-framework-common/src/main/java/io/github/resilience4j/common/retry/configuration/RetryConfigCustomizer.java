package io.github.resilience4j.common.retry.configuration;

import io.github.resilience4j.retry.RetryConfig;

/**
 * Enable customization retry configuration builders programmatically.
 */
public interface RetryConfigCustomizer {

    /**
     * Retry configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(RetryConfig.Builder configBuilder);

    /**
     * @return name of the retry instance to be customized
     */
    String name();
}
