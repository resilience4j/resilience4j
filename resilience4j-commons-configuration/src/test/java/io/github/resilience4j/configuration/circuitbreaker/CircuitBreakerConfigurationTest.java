package io.github.resilience4j.configuration.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

public class CircuitBreakerConfigurationTest {
    private static final CircuitBreakerConfig DEFAULT_CONFIG = CircuitBreakerConfig.ofDefaults();
    private Configuration aConfiguration;

    @Before
    public void initializeXmlConfiguration() throws ConfigurationException {
        aConfiguration = spy(new Configurations().xml(this.getClass().getClassLoader().getResource("circuitbreaker-config.xml")));
    }

    @Test
    public void testGetDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.getDefault();

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetConfiguredAutoTransitionWithDefaultsFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("autoTransitionFalse");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isFalse();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetConfiguredFailureRateWithDefaultsFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("failureRate");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(3.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetConfiguredMinimumCallsWithDefaultsFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("minimumCalls");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(4);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetConfiguredNumberInHalfOpenStateWithDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("numberInHalfOpen");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(5);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetSlidingWindowSizeWithDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("slidingWindowSize");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(6);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetCountBasedWindowWithDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("countBasedWindow");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetSlowCallDurationThresholdWithDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("slowCallDuration");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(8));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetSlowCallRateThresholdWithDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("slowCallRateThreshold");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(9.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetWaitDurationInOpenStateWithDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("waitDurationOpen");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(10).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetWriteableStackTraceDisabledWithDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
        CircuitBreakerConfig config = configuration.get("writableStack");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(1);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1);
        assertThat(config.getSlidingWindowSize()).isEqualTo(1);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(1.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(1).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isFalse();
    }

    @Test
    public void testGetDefaultWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.getDefault();

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetUnconfiguredWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("unconfigured");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredAutoTransitionWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("autoTransitionTrue");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredFailureRateThresholdWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("failureRate");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(13.0f);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredMinimumNumberOfCallsWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("minimumCalls");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(14);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredPermittedNumberOfCallsInHalfOpenStateWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("numberInHalfOpen");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(15);
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredSlidingWindowSizeWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("slidingWindowSize");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(16);
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredSlidingWindowTypeWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("timeBasedWindow");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(TIME_BASED);
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredSlowCallDurationThresholdWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("slowCallDuration");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(18));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredSlowCallRateThresholdWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("slowCallRateThreshold");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(19);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredWaitDurationInOpenStateWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("waitDurationOpen");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(20).toMillis()); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetConfiguredWritableStackTraceEnabledWithNoDefaultFromConfiguration() {
        given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

        CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
        CircuitBreakerConfig config = configuration.get("writableStack");

        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(DEFAULT_CONFIG.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        assertThat(config.getFailureRateThreshold()).isEqualTo(DEFAULT_CONFIG.getFailureRateThreshold());
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(DEFAULT_CONFIG.getMinimumNumberOfCalls());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(DEFAULT_CONFIG.getPermittedNumberOfCallsInHalfOpenState());
        assertThat(config.getSlidingWindowSize()).isEqualTo(DEFAULT_CONFIG.getSlidingWindowSize());
        assertThat(config.getSlidingWindowType()).isEqualByComparingTo(DEFAULT_CONFIG.getSlidingWindowType());
        assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(DEFAULT_CONFIG.getSlowCallDurationThreshold());
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(DEFAULT_CONFIG.getSlowCallRateThreshold());
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(DEFAULT_CONFIG.getWaitIntervalFunctionInOpenState().apply(1)); // the duration in milliseconds
        assertThat(config.isWritableStackTraceEnabled()).isTrue();
    }
}
