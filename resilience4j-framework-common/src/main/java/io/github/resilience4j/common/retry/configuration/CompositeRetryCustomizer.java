package io.github.resilience4j.common.retry.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * the composite  of any retry {@link RetryConfigCustomizer} implementations.
 */
public class CompositeRetryCustomizer {

    private final Map<String, RetryConfigCustomizer> customizerMap = new HashMap<>();

    public CompositeRetryCustomizer(List<RetryConfigCustomizer> customizers) {

        if (customizers != null && !customizers.isEmpty()) {
            customizerMap.putAll(customizers.stream()
                .collect(
                    Collectors.toMap(RetryConfigCustomizer::name, Function.identity())));
        }

    }

    /**
     * @param retryInstanceName the retry instance name
     * @return the found {@link RetryConfigCustomizer} if any .
     */
    public Optional<RetryConfigCustomizer> getRetryConfigCustomizer(
        String retryInstanceName) {
        return Optional.ofNullable(customizerMap.get(retryInstanceName));
    }

}
