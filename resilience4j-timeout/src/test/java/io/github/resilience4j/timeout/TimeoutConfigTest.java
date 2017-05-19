package io.github.resilience4j.timeout;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

import static java.lang.Boolean.FALSE;
import static org.assertj.core.api.BDDAssertions.then;

public class TimeoutConfigTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Boolean CANCEL_ON_EXECUTION = FALSE;
    private static final String TIMEOUT_DURATION_MUST_NOT_BE_NULL = "TimeoutDuration must not be null";
    private static final String CANCEL_ON_EXCEPTION_MUST_NOT_BE_NULL = "CancelOnExecution must not be null";

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void builderPositive() throws Exception {
        TimeoutConfig config = TimeoutConfig.custom()
                .timeoutDuration(TIMEOUT)
                .cancelOnException(CANCEL_ON_EXECUTION)
                .build();

        then(config.getTimeoutDuration()).isEqualTo(TIMEOUT);
        then(config.shouldCancelOnException()).isEqualTo(CANCEL_ON_EXECUTION);
    }

    @Test
    public void builderTimeoutIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(TIMEOUT_DURATION_MUST_NOT_BE_NULL);
        TimeoutConfig.custom()
                .timeoutDuration(null);

        exception.expect(NullPointerException.class);
        exception.expectMessage(CANCEL_ON_EXCEPTION_MUST_NOT_BE_NULL);
    }

}
