package io.github.resilience4j.timelimiter;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

class TimeLimiterConfigTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final boolean SHOULD_CANCEL = false;
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String TIMEOUT_TO_STRING = "TimeLimiterConfig{timeoutDuration=PT1ScancelRunningFuture=true}";

    @Test
    void builderPositive() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .cancelRunningFuture(SHOULD_CANCEL)
            .build();

        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.shouldCancelRunningFuture()).isEqualTo(SHOULD_CANCEL);
    }

    @Test
    void defaultConstruction() {
        TimeLimiterConfig config = TimeLimiterConfig.ofDefaults();
        then(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(1));
        then(config.shouldCancelRunningFuture()).isTrue();
    }

    @Test
    void builderTimeoutIsNull() {
        assertThatThrownBy(() -> TimeLimiterConfig.custom().timeoutDuration(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(TIMEOUT_DURATION_MUST_NOT_BE_NULL);
    }

    @Test
    void configToString() {
        then(TimeLimiterConfig.ofDefaults().toString()).isEqualTo(TIMEOUT_TO_STRING);
    }

    @Test
    void shouldUseBaseConfigAndOverwriteProperties() {
        TimeLimiterConfig baseConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .cancelRunningFuture(false)
            .build();

        TimeLimiterConfig extendedConfig = TimeLimiterConfig.from(baseConfig)
            .timeoutDuration(Duration.ofSeconds(20))
            .build();

        then(extendedConfig.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(20));
        then(extendedConfig.shouldCancelRunningFuture()).isFalse();
    }
}
