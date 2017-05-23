package io.github.resilience4j.timeout;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

public class TimeoutConfigTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void builderPositive() throws Exception {
        TimeoutConfig config = TimeoutConfig.custom()
                .timeoutDuration(TIMEOUT)
                .build();

        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
    }

    @Test
    public void builderTimeoutIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(TIMEOUT_DURATION_MUST_NOT_BE_NULL);

        TimeoutConfig.custom()
                .timeoutDuration(null);
    }

}
