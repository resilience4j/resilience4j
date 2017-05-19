package io.github.resilience4j.timeout.internal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;

import io.github.resilience4j.timeout.TimeoutConfig;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TimeoutContext.class)
public class TimeoutContextTest {

    private TimeoutConfig timeoutConfig;
    private TimeoutContext timeout;

    @Before
    public void setup() {
        timeoutConfig = TimeoutConfig.custom()
                .timeoutDuration(Duration.ZERO)
                .build();
        TimeoutContext testTimeout = new TimeoutContext(timeoutConfig);
        timeout = PowerMockito.spy(testTimeout);
    }

    @Test
    public void configPropagation() {
        then(timeout.getTimeoutConfig()).isEqualTo(timeoutConfig);
    }
}
