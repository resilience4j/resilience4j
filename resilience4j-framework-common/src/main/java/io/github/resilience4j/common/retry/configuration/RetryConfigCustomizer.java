package io.github.resilience4j.common.retry.configuration;

import io.github.resilience4j.common.CustomizerWithName;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.retry.RetryConfig;

import java.util.function.Consumer;

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

    /**
     * A convenient method to create RetryConfigCustomizer using {@link Consumer}
     *
     * @param instanceName the name of the instance
     * @param consumer     delegate call to Consumer when  {@link RetryConfigCustomizer#customize(RetryConfig.Builder)}
     *                     is called
     * @param <T>          generic type of Customizer
     * @return Customizer instance
     */
    static <T> RetryConfigCustomizer of(@NonNull String instanceName,
        @NonNull Consumer<RetryConfig.Builder> consumer) {
        return new RetryConfigCustomizer() {

            @Override
            public void customize(RetryConfig.Builder builder) {
                consumer.accept(builder);
            }

            @Override
            public String name() {
                return instanceName;
            }
        };
    }
}
