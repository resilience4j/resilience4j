package io.github.resilience4j.configuration.utils;

import org.apache.commons.configuration2.Configuration;

import java.time.Duration;

public class ConfigurationUtil {
    public static Duration getDuration(final Configuration configuration, final String key, final Duration defaultDuration) {
        String durationString = configuration.getString(key, defaultDuration.toString());
        return Duration.parse(durationString);
    }
}
