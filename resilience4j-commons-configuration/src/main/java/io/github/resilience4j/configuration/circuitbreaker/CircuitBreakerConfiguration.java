package io.github.resilience4j.configuration.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.configuration.InheritedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;

/**
 * {@link CircuitBreakerConfiguration} is used to create {@link CircuitBreakerConfig} from a {@link Configuration}.
 * <p>
 * Each instance of CircuitBreakerConfiguration is used to access properties for a specific named {@link Configuration#subset(String) subset} from a specific
 * contextual subset of the Configuration. The {@code default contextual subset} is accessed by the prefix
 * {@code io.github.resilience4j.circuitbreaker}, and the {@code default named subset} prefix is {@code default}.
 * </p>
 * <p>
 * A CircuitBreakerConfiguration may access properties for one or more named subsets within the contextual subset.
 * </p>
 */
public class CircuitBreakerConfiguration extends InheritedConfiguration<CircuitBreakerConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerConfiguration.class);
    private static final String DEFAULT_CONTEXT = "io.github.resilience4j.circuitbreaker";

    /**
     * Initializes a {@link CircuitBreakerConfiguration} using the provided {@code config} and the
     * {@code default contextual subset}.
     *
     * @param config the Configuration.
     * @see #CircuitBreakerConfiguration(Configuration, String)
     */
    public CircuitBreakerConfiguration(final Configuration config) {
        this(config, DEFAULT_CONTEXT);
    }

    /**
     * Initializes a {@link CircuitBreakerConfiguration} using the provided {@code config} and {@code context}
     * {@link Configuration#subset(String) subset}.
     *
     * @param config  the Configuration.
     * @param context the contextual prefix from which named circuit breaker configuration will be accessed.
     */
    public CircuitBreakerConfiguration(final Configuration config, final String context) {
        super(config, context);
    }

    @Override
    protected CircuitBreakerConfig map(Configuration config, CircuitBreakerConfig defaults) {
        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom()
            .automaticTransitionFromOpenToHalfOpenEnabled(config.getBoolean("automaticTransitionFromOpenToHalfOpenEnabled", defaults.isAutomaticTransitionFromOpenToHalfOpenEnabled()))
            .failureRateThreshold(config.getFloat("failureRateThreshold", defaults.getFailureRateThreshold()))
            .minimumNumberOfCalls(config.getInt("minimumNumberOfCalls", defaults.getMinimumNumberOfCalls()))
            .permittedNumberOfCallsInHalfOpenState(config.getInt("permittedNumberOfCallsInHalfOpenState", defaults.getPermittedNumberOfCallsInHalfOpenState()))
            .slidingWindowSize(config.getInt("slidingWindowSize", defaults.getSlidingWindowSize()))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.valueOf(config.getString("slidingWindowType", defaults.getSlidingWindowType().name())))
            .slowCallDurationThreshold(getDuration(config, "slowCallDurationThreshold", defaults.getSlowCallDurationThreshold()))
            .slowCallRateThreshold(config.getFloat("slowCallRateThreshold", defaults.getSlowCallRateThreshold()))
            .waitDurationInOpenState(getDuration(config, "waitDurationInOpenState", Duration.ofMillis(defaults.getWaitIntervalFunctionInOpenState().apply(1))))
            .writableStackTraceEnabled(config.getBoolean("writableStackTraceEnabled", defaults.isWritableStackTraceEnabled()));
        builder = setOrDefaultIgnoreExceptions(builder, config, defaults);
        builder = setOrDefaultRecordExceptions(builder, config, defaults);

        return builder.build();
    }

    @Override
    protected CircuitBreakerConfig getDefaultConfigObject() {
        return CircuitBreakerConfig.ofDefaults();
    }

    protected Logger getLogger() {
        return LOG;
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
