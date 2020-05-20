package io.github.resilience4j.configuration.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.EOFException;
import java.io.IOException;
import java.time.Duration;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

@RunWith(Enclosed.class)
public class CircuitBreakerConfigurationTest {
    private static final CircuitBreakerConfig DEFAULT_CONFIG = CircuitBreakerConfig.ofDefaults();

    protected static Configuration aConfiguration;

    @BeforeClass
    public static void initializeXmlConfiguration() throws ConfigurationException {
        aConfiguration = spy(new Configurations().xml(CircuitBreakerConfigurationTest.class.getClassLoader().getResource("circuitbreaker-config.xml")));
    }

    public static class ConfigurationFromDefault {
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new IOException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
        }

        @Test
        public void testGetIgnoreExceptionsWithDefaultFromConfiguration() {
            given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
            given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
            given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
            given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

            CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
            CircuitBreakerConfig config = configuration.get("ignore");

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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isFalse();
        }

        @Test
        public void testGetRecordExceptionsWithDefaultFromConfiguration() {
            given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
            given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
            given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
            given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

            CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration);
            CircuitBreakerConfig config = configuration.get("record");

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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new EOFException())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new RuntimeException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new EOFException())).isTrue();
        }
    }

    public static class ConfigurationWithNoDefault {
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
        }

        @Test
        public void testGetIgnoreExceptionstWithNoDefaultFromConfiguration() {
            given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
            given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
            given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
            given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

            CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
            CircuitBreakerConfig config = configuration.get("ignore");

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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new Exception())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isTrue();
        }

        @Test
        public void testGetRecordExceptionsWithNoDefaultFromConfiguration() {
            given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
            given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
            given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
            given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

            CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "custom.noDefault.circuitbreaker");
            CircuitBreakerConfig config = configuration.get("record");

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
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new Exception())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new RuntimeException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
        }
    }

    public static class ConfigurationFromBase {
        @Test
        public void testGetRecordExceptionsWithNoDefaultFromConfiguration() {
            given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
            given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
            given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
            given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

            CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "inherited");
            CircuitBreakerConfig config = configuration.get("duration");

            assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
            assertThat(config.getFailureRateThreshold()).isEqualTo(20f);
            assertThat(config.getMinimumNumberOfCalls()).isEqualTo(27);
            assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(28);
            assertThat(config.getSlidingWindowSize()).isEqualTo(29);
            assertThat(config.getSlidingWindowType()).isEqualByComparingTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
            assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(30));
            assertThat(config.getSlowCallRateThreshold()).isEqualTo(31);
            assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(32).toMillis()); // the duration in milliseconds
            assertThat(config.isWritableStackTraceEnabled()).isTrue();
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new Exception())).isTrue();
            assertThat(config.getRecordExceptionPredicate().test(new RuntimeException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new IOException())).isTrue();
        }
        @Test
        public void testInheritance() {
            given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
            given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
            given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
            given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

            CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "inherited");
            CircuitBreakerConfig config = configuration.get("duration");

            assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
            assertThat(config.getFailureRateThreshold()).isEqualTo(20f);
            assertThat(config.getMinimumNumberOfCalls()).isEqualTo(27);
            assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(28);
            assertThat(config.getSlidingWindowSize()).isEqualTo(29);
            assertThat(config.getSlidingWindowType()).isEqualByComparingTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
            assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(30));
            assertThat(config.getSlowCallRateThreshold()).isEqualTo(31);
            assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(32).toMillis()); // the duration in milliseconds
            assertThat(config.isWritableStackTraceEnabled()).isTrue();
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new Exception())).isTrue();
            assertThat(config.getRecordExceptionPredicate().test(new RuntimeException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new IOException())).isTrue();
        }

        @Test
        public void testMultiInheritance() {
            given(aConfiguration.getFloat(anyString(), anyFloat())).willCallRealMethod();
            given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();
            given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
            given(aConfiguration.getString(anyString(), anyString())).willCallRealMethod();

            CircuitBreakerConfiguration configuration = new CircuitBreakerConfiguration(aConfiguration, "inherited");
            CircuitBreakerConfig config = configuration.get("record");

            assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
            assertThat(config.getFailureRateThreshold()).isEqualTo(20f);
            assertThat(config.getMinimumNumberOfCalls()).isEqualTo(27);
            assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(28);
            assertThat(config.getSlidingWindowSize()).isEqualTo(29);
            assertThat(config.getSlidingWindowType()).isEqualByComparingTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
            assertThat(config.getSlowCallDurationThreshold()).isEqualByComparingTo(Duration.ofSeconds(24));
            assertThat(config.getSlowCallRateThreshold()).isEqualTo(25);
            assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(26).toMillis()); // the duration in milliseconds
            assertThat(config.isWritableStackTraceEnabled()).isTrue();
            assertThat(config.getRecordExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new Exception())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new RuntimeException())).isFalse();
            assertThat(config.getRecordExceptionPredicate().test(new IOException())).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(new Throwable())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new Exception())).isFalse();
            assertThat(config.getIgnoreExceptionPredicate().test(new RuntimeException())).isTrue();
        }
    }
}