package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TimeLimiterImpl.class)
public class TimeLimiterImplTest {

    private static final String NAME = "name";
    private TimeLimiterConfig timeLimiterConfig;
    private TimeLimiter timeLimiter;

    @Before
    public void init() {
        timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ZERO)
            .build();
        TimeLimiterImpl testTimeout = new TimeLimiterImpl("name", timeLimiterConfig);
        timeLimiter = PowerMockito.spy(testTimeout);
    }

    @Test
    public void configPropagation() {
        then(timeLimiter.getTimeLimiterConfig()).isEqualTo(timeLimiterConfig);
    }

    @Test
    public void namePropagation() {
        then(timeLimiter.getName()).isEqualTo(NAME);
    }
}
