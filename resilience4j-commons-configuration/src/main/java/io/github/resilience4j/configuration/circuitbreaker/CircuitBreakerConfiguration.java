package io.github.resilience4j.configuration.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.apache.commons.configuration2.Configuration;

import static io.github.resilience4j.configuration.utils.ConfigurationUtil.getDuration;

/**
 * {@link CircuitBreakerConfiguration} is used to create {@link CircuitBreakerConfig} from a {@link Configuration}.
 */
public class CircuitBreakerConfiguration {
    private static final String DEFAULT_NAME = "default";
    private static final String DEFAULT_CONTEXT = "io.github.resilience4j.circuitbreaker";
    private final Configuration config;

    public CircuitBreakerConfiguration(final Configuration config) {
        this(config, DEFAULT_CONTEXT);
    }

    public CircuitBreakerConfiguration(final Configuration config, final String circuitBreakerPrefix) {
        this.config = config.subset(circuitBreakerPrefix);
    }

    public CircuitBreakerConfig get(String name, final CircuitBreakerConfig defaultConfig) {
        final Configuration namedConfig = config.subset(name);

        return CircuitBreakerConfig.custom()
            .automaticTransitionFromOpenToHalfOpenEnabled(namedConfig.getBoolean("automaticTransitionFromOpenToHalfOpenEnabled", defaultConfig.isAutomaticTransitionFromOpenToHalfOpenEnabled()))
            .failureRateThreshold(namedConfig.getFloat("failureRateThreshold", defaultConfig.getFailureRateThreshold()))
            .minimumNumberOfCalls(namedConfig.getInt("minimumNumberOfCalls", defaultConfig.getMinimumNumberOfCalls()))
            .permittedNumberOfCallsInHalfOpenState(namedConfig.getInt("permittedNumberOfCallsInHalfOpenState", defaultConfig.getPermittedNumberOfCallsInHalfOpenState()))
            .slidingWindowSize(namedConfig.getInt("slidingWindowSize", defaultConfig.getSlidingWindowSize()))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(namedConfig.getString("slidingWindowType", defaultConfig.getSlidingWindowType().name())))
            .slowCallDurationThreshold(getDuration(namedConfig,"slowCallDurationThreshold", defaultConfig.getSlowCallDurationThreshold()))
            .slowCallRateThreshold(namedConfig.getFloat("slowCallRateThreshold", defaultConfig.getSlowCallRateThreshold()))
            .waitDurationInOpenState(getDuration(namedConfig, "waitDurationInOpenState", defaultConfig.getWaitDurationInOpenState()))
            .writableStackTraceEnabled(namedConfig.getBoolean("writableStackTraceEnabled", defaultConfig.isWritableStackTraceEnabled()))
            //.ignoreExceptions() // TODO - read Strings and load classes
            //.recordExceptions() // TODO - read Strings and load classes
            .build();
    }

    public CircuitBreakerConfig get(String name) {
        return get(name, getDefault());
    }

    public CircuitBreakerConfig getDefault() {
        return get(DEFAULT_NAME, CircuitBreakerConfig.ofDefaults());
    }
}
