package io.github.resilience4j.timelimiter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

public class TimeLimiterConfigTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final boolean SHOULD_CANCEL = false;
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String TIMEOUT_TO_STRING = "TimeLimiterConfig{timeoutDuration=PT1ScancelRunningFuture=true}";

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void builderPositive() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .cancelRunningFuture(SHOULD_CANCEL)
            .build();

        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.shouldCancelRunningFuture()).isEqualTo(SHOULD_CANCEL);
    }

    @Test
    public void defaultConstruction() {
        TimeLimiterConfig config = TimeLimiterConfig.ofDefaults();
        then(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(1));
        then(config.shouldCancelRunningFuture()).isTrue();
    }

    @Test
    public void builderTimeoutIsNull() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(TIMEOUT_DURATION_MUST_NOT_BE_NULL);

        TimeLimiterConfig.custom()
            .timeoutDuration(null);
    }

    @Test
    public void configToString() {
        then(TimeLimiterConfig.ofDefaults().toString()).isEqualTo(TIMEOUT_TO_STRING);
    }
}
