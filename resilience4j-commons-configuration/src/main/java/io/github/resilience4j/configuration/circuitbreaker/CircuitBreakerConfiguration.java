package io.github.resilience4j.configuration.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.apache.commons.configuration2.Configuration;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;

import static io.github.resilience4j.configuration.utils.ConfigurationUtil.getDuration;
import static io.github.resilience4j.configuration.utils.ConfigurationUtil.getThrowableClassesByName;

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

        CircuitBreakerConfig baseConfig =
            Optional.ofNullable(namedConfig.getString("baseConfig")).map(b -> {
                return this.get(b, defaultConfig);
            }).orElse(defaultConfig);

        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom()
            .automaticTransitionFromOpenToHalfOpenEnabled(namedConfig.getBoolean("automaticTransitionFromOpenToHalfOpenEnabled", baseConfig.isAutomaticTransitionFromOpenToHalfOpenEnabled()))
            .failureRateThreshold(namedConfig.getFloat("failureRateThreshold", baseConfig.getFailureRateThreshold()))
            .minimumNumberOfCalls(namedConfig.getInt("minimumNumberOfCalls", baseConfig.getMinimumNumberOfCalls()))
            .permittedNumberOfCallsInHalfOpenState(namedConfig.getInt("permittedNumberOfCallsInHalfOpenState", baseConfig.getPermittedNumberOfCallsInHalfOpenState()))
            .slidingWindowSize(namedConfig.getInt("slidingWindowSize", baseConfig.getSlidingWindowSize()))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(namedConfig.getString("slidingWindowType", baseConfig.getSlidingWindowType().name())))
            .slowCallDurationThreshold(getDuration(namedConfig, "slowCallDurationThreshold", baseConfig.getSlowCallDurationThreshold()))
            .slowCallRateThreshold(namedConfig.getFloat("slowCallRateThreshold", baseConfig.getSlowCallRateThreshold()))
            .waitDurationInOpenState(getDuration(namedConfig, "waitDurationInOpenState", Duration.ofMillis(baseConfig.getWaitIntervalFunctionInOpenState().apply(1))))
            .writableStackTraceEnabled(namedConfig.getBoolean("writableStackTraceEnabled", baseConfig.isWritableStackTraceEnabled()));
        builder = setOrDefaultIgnoreExceptions(builder, namedConfig, baseConfig);
        builder = setOrDefaultRecordExceptions(builder, namedConfig, baseConfig);

        return builder.build();
    }

    public CircuitBreakerConfig get(String name) {
        return get(name, getDefault());
    }

    public CircuitBreakerConfig getDefault() {
        return get(DEFAULT_NAME, CircuitBreakerConfig.ofDefaults());
    }

    private CircuitBreakerConfig.Builder setOrDefaultIgnoreExceptions(final CircuitBreakerConfig.Builder configBuilder, Configuration config, CircuitBreakerConfig defaultConfig) {
        Collection<Class<?>> configuredValues = getThrowableClassesByName(config, "ignoreExceptions");
        if (configuredValues == null) {
            return configBuilder.ignoreException(defaultConfig.getIgnoreExceptionPredicate());
        }

        return configBuilder.ignoreExceptions((Class<? extends Throwable>[]) configuredValues.toArray(new Class<?>[0]));
    }

    private CircuitBreakerConfig.Builder setOrDefaultRecordExceptions(final CircuitBreakerConfig.Builder configBuilder, Configuration config, CircuitBreakerConfig defaults) {
        Collection<Class<?>> configuredValues = getThrowableClassesByName(config, "recordExceptions");
        if (configuredValues == null) {
            return configBuilder.recordException(defaults.getRecordExceptionPredicate());
        }

        return configBuilder.recordExceptions((Class<? extends Throwable>[]) configuredValues.toArray(new Class<?>[0]));
    }
}
