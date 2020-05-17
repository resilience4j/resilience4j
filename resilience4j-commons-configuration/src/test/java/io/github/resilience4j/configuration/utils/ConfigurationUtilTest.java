package io.github.resilience4j.configuration.utils;

import org.apache.commons.configuration2.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationUtilTest {
    private static final String key = "duration";
    private static final Duration defaultDuration = Duration.ofDays(365);

    @Mock
    public Configuration aConfiguration;

    @Test
    public void testValidISO8601Duration() {
        given(aConfiguration.getString(key, defaultDuration.toString())).willReturn("PT30S");

        Duration duration = ConfigurationUtil.getDuration(aConfiguration, key, defaultDuration);

        assertThat(duration).isEqualByComparingTo(Duration.ofSeconds(30));
    }

    @Test
    public void testInvalidISO8601Duration() {
        given(aConfiguration.getString(key, defaultDuration.toString())).willReturn("30 SECONDS");

        assertThatThrownBy(() -> {
            ConfigurationUtil.getDuration(aConfiguration, key, defaultDuration);
        }).isInstanceOf(DateTimeParseException.class);
    }

    @Test
    public void testUnconfiguredDuration() {
        given(aConfiguration.getString(key, defaultDuration.toString())).willReturn(defaultDuration.toString());

        Duration duration = ConfigurationUtil.getDuration(aConfiguration, key, defaultDuration);

        assertThat(duration).isEqualByComparingTo(defaultDuration);
    }

    @Test
    public void testGetThrowableClassesByNameNoDefaults() {
        given(aConfiguration.getStringArray(key)).willReturn(new String[]{"java.lang.RuntimeException", "java.io.IOException"});

        Set<Class<?>> throwables = ConfigurationUtil.getThrowableClassesByName(aConfiguration, key);

        assertThat(throwables).containsOnly(RuntimeException.class, IOException.class);
    }

    @Test
    public void testGetThrowableClassesByNameWithDefaults() {
        given(aConfiguration.getStringArray(key)).willReturn(new String[]{"java.lang.RuntimeException", "java.io.IOException"});

        Set<Class<?>> throwables = ConfigurationUtil.getThrowableClassesByName(aConfiguration, key, NullPointerException.class);

        assertThat(throwables).containsOnly(RuntimeException.class, IOException.class);
    }

    @Test
    public void testGetThrowableClassesByNameNoConfiguredValuesNoDefaults() {
        given(aConfiguration.getStringArray(key)).willReturn(new String[]{});

        Set<Class<?>> throwables = ConfigurationUtil.getThrowableClassesByName(aConfiguration, key);

        assertThat(throwables).isNull();
    }

    @Test
    public void testGetThrowableClassesByNameNoConfiguredValuesWithDefaults() {
        given(aConfiguration.getStringArray(key)).willReturn(new String[]{});

        Set<Class<?>> throwables = ConfigurationUtil.getThrowableClassesByName(aConfiguration, key, NullPointerException.class);

        assertThat(throwables).containsOnly(NullPointerException.class);
    }

    @Test
    public void testGetThrowableClassesByNameConfiguredValuesNotThrowablesNoDefaults() {
        given(aConfiguration.getStringArray(key)).willReturn(new String[]{this.getClass().getName()});

        Set<Class<?>> throwables = ConfigurationUtil.getThrowableClassesByName(aConfiguration, key);

        assertThat(throwables).isNull();
    }
}
