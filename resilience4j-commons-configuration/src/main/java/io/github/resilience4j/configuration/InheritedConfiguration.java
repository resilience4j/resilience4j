package io.github.resilience4j.configuration;

import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link InheritedConfiguration} is an abstract class for reading configuration from a {@link Configuration} with an
 * inheritance structure.
 * <p>
 * InheritedConfiguration is used to access properties for a specific named {@link Configuration#subset(String) subset}
 * from a specific contextual subset of the Configuration. InheritedConfiguration can be used to load a default config
 * from the context named {@code default}. If present, any configurations at the same subset as a {@code default} config
 * will inherit the values of the default. Additionally, any configuration may specify a {@code baseConfig} name within
 * its same subset and its values will be inherited.
 * </p>
 *
 * @param <T> The type of config to be read.
 */
public abstract class InheritedConfiguration<T> {
    private static final String DEFAULT_NAME = "default";
    protected final Configuration config;

    /**
     * Initializes an {@link InheritedConfiguration} using the provided {@code config} and {@code context}
     * {@link Configuration#subset(String) subset},
     *
     * @param config  the Configuration.
     * @param context the contextual prefix from which named circuit breaker configuration will be accessed.
     */
    protected InheritedConfiguration(final Configuration config, final String context) {
        this.config = config.subset(context);
    }

    /**
     * Creates a config of type {@link T} with the properties accessed at the {@code default named subset}. If a
     * property is not accessible in the Configuration, the value will be replicated from the
     * {@link #getDefaultConfigObject() default config}.
     *
     * @return the default configured config of type T
     * @see #get(String, Object)
     */
    public T getDefault() {
        return get(DEFAULT_NAME, getDefaultConfigObject());
    }

    /**
     * Creates a config of type {@link T} with the properties accessed at the {@link Configuration#subset(String) subset}
     * with the provided {@code name}. If a property is not accessible in the Configuration, the value will be
     * replicated from the {@link #getDefault() configured default}.
     *
     * @param name the subset prefix of the Configuration from which to create the config.
     * @return a config of type T
     * @see #get(String, Object)
     * @see #getDefaultConfigObject()
     */
    public T get(String name) {
        return get(name, getDefault());
    }

    /**
     * Creates a config of type {@link T} with the properties accessed at the {@link Configuration#subset(String) subset}
     * with the provided {@code name}. If a property is not accessible in the Configuration, the value will be
     * replicated from the provided {@code defaults}.
     *
     * @param name     the subset prefix of the contextual Configuration subset from which to create the config object.
     * @param defaults the config object used to provide values for unconfigured properties.
     * @return a config of type T
     * @see #getDefaultConfigObject()
     */
    public T get(String name, T defaults) {
        final Configuration namedConfig = config.subset(name);

        T baseConfig =
            Optional.ofNullable(namedConfig.getString("baseConfig")).map(b -> {
                return this.get(b, defaults);
            }).orElse(defaults);

        return map(namedConfig, baseConfig);
    }

    /**
     * Convenience method for reading a {@link Duration} from {@link Configuration}.
     *
     * @param configuration   the Configuration object from which the Duration will be read.
     * @param key             the Configuration key containing the Duration.
     * @param defaultDuration the default Duration to use if there is no value in the {@code configuration} for the
     *                        {@code key}.
     * @return a Duration
     */
    protected Duration getDuration(final Configuration configuration, final String key, final Duration defaultDuration) {
        String durationString = configuration.getString(key, defaultDuration.toString());
        return Duration.parse(durationString);
    }

    /**
     * Convenience method for reading {@link Class Classes} of type {@link Throwable} by fully-qualified class name.
     * Classes which are not a subtype of Throwable are omitted.
     * <p>
     * The return value may be {@code null} if there are no configured values and no provided {@code defaultClasses}.
     * The set may be empty if there are no classes of type Throwable configured and no {@code defaultClasses} are
     * provided.
     * </p>
     *
     * @param configuration  the Configuration object from which the Duration will be read.
     * @param key            the Configuration key containing the Duration.
     * @param defaultClasses the default classes to use if there is no value in the {@code configuration} for the
     *                       {@code key}.
     * @return a possibly null, possibly empty {@link Set} of {@link Throwable} {@link Class Classes}.
     */
    protected Set<Class<?>> getThrowableClassesByName(final Configuration configuration, final String key, final Class<? extends Throwable>... defaultClasses) {
        Set<Class<?>> exceptionClasses = Arrays.stream(configuration.getStringArray(key)).map(s -> {
            try {
                Class<?> clazz = Class.forName(s);
                getLogger().debug("Loaded class {}", s);
                return clazz;
            } catch (ClassNotFoundException e) {
                getLogger().warn("Class {} cannot be loaded", s, e);
            }
            return null;
        }).filter(Objects::nonNull).filter(c -> {
            return Throwable.class.isAssignableFrom(c);
        }).collect(Collectors.toSet());

        if (!exceptionClasses.isEmpty()) {
            getLogger().debug("Returning configured values");
            return new HashSet<Class<?>>(exceptionClasses);
        } else if (defaultClasses.length > 0) {
            getLogger().debug("Returning default values");
            return new HashSet<Class<?>>(Arrays.asList(defaultClasses));
        }

        getLogger().debug("No configured values and no defaults provided");
        return null;
    }

    /**
     * Maps values from the {@code config} to the config object, using the values from the {@code defaults} if the value
     * does not exist in the {@code config}.
     *
     * @param config   the Configuration object from which the values will be read.
     * @param defaults the default whose values will be used in the absence of any values in the {@code config}.
     * @return a config of type T
     */
    abstract protected T map(Configuration config, T defaults);

    /**
     * The global default config object to use in the absence of any configured values.
     *
     * @return a config of type T.
     */
    abstract protected T getDefaultConfigObject();

    /**
     * The {@code Logger} for the implementation.
     *
     * @return a Logger.
     */
    abstract protected Logger getLogger();
}
