package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.spy;

class TimeLimiterImplTest {

    private static final String NAME = "name";
    private TimeLimiterConfig timeLimiterConfig;
    private TimeLimiter timeLimiter;

    @BeforeEach
    void init() {
        timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ZERO)
            .build();
        TimeLimiterImpl testTimeout = new TimeLimiterImpl("name", timeLimiterConfig);
        timeLimiter = spy(testTimeout);
    }

    @Test
    void configPropagation() {
        then(timeLimiter.getTimeLimiterConfig()).isEqualTo(timeLimiterConfig);
    }

    @Test
    void namePropagation() {
        then(timeLimiter.getName()).isEqualTo(NAME);
    }
}
