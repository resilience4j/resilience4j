package io.github.resilience4j.configuration.utils;

import org.apache.commons.configuration2.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationUtilTest {
    private static final String key = "duration";
    private static final Duration defaultDuration = Duration.ofDays(365);

    @Mock
    public Configuration configuration;

    @Test
    public void testValidISO8601Duration() {
        given(configuration.getString(key, defaultDuration.toString())).willReturn("PT30S");

        Duration duration = ConfigurationUtil.getDuration(configuration, key, defaultDuration);

        assertThat(duration).isEqualByComparingTo(Duration.ofSeconds(30));
    }

    @Test
    public void testInvalidISO8601Duration() {
        given(configuration.getString(key, defaultDuration.toString())).willReturn("30 SECONDS");

        assertThatThrownBy(() -> {
            ConfigurationUtil.getDuration(configuration, key, defaultDuration);
        }).isInstanceOf(DateTimeParseException.class);
    }

    @Test
    public void testUnconfigured() {
        given(configuration.getString(key, defaultDuration.toString())).willReturn(defaultDuration.toString());

        Duration duration = ConfigurationUtil.getDuration(configuration, key, defaultDuration);

        assertThat(duration).isEqualByComparingTo(defaultDuration);
    }
}
