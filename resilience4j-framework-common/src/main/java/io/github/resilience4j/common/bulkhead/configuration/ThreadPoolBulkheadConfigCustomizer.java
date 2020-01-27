package io.github.resilience4j.common.bulkhead.configuration;

import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.common.CustomizerWithName;
import io.github.resilience4j.core.lang.NonNull;

import java.util.function.Consumer;

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

    /**
     * A convenient method to create ThreadpoolBulkheadConfigCustomizer using {@link Consumer}
     *
     * @param instanceName the name of the instance
     * @param consumer     delegate call to Consumer when  {@link ThreadPoolBulkheadConfigCustomizer#customize(ThreadPoolBulkheadConfig.Builder)}
     *                     is called
     * @param <T>          generic type of Customizer
     * @return Customizer instance
     */
    static <T> ThreadPoolBulkheadConfigCustomizer of(@NonNull String instanceName,
        @NonNull Consumer<ThreadPoolBulkheadConfig.Builder> consumer) {
        return new ThreadPoolBulkheadConfigCustomizer() {

            @Override
            public void customize(ThreadPoolBulkheadConfig.Builder builder) {
                consumer.accept(builder);
            }

            @Override
            public String name() {
                return instanceName;
            }
        };
    }
}
