package io.github.resilience4j.configuration.utils;

import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigurationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUtil.class);

    private ConfigurationUtil() {
    }

    public static Duration getDuration(final Configuration configuration, final String key, final Duration defaultDuration) {
        String durationString = configuration.getString(key, defaultDuration.toString());
        return Duration.parse(durationString);
    }

    public static Set<Class<?>> getThrowableClassesByName(final Configuration configuration, final String key, final Class<? extends Throwable>... defaultClasses) {
        Set<Class<?>> exceptionClasses = Arrays.stream(configuration.getStringArray(key)).map(s -> {
            try {
                Class<?> clazz = Class.forName(s);
                LOG.debug("Loaded class {}", s);
                return clazz;
            } catch (ClassNotFoundException e) {
                LOG.warn("Class {} cannot be loaded", s, e);
            }
            return null;
        }).filter(Objects::nonNull).filter(c -> {
            return Throwable.class.isAssignableFrom(c);
        }).collect(Collectors.toSet());

        if (!exceptionClasses.isEmpty()) {
            LOG.debug("Returning configured values");
            return new HashSet<Class<?>>(exceptionClasses);
        } else if (defaultClasses.length > 0) {
            LOG.debug("Returning default values");
            return new HashSet<Class<?>>(Arrays.asList(defaultClasses));
        }

        LOG.debug("No configured values and no defaults provided");
        return null;
    }
}
