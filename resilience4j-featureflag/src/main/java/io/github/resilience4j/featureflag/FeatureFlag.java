package io.github.resilience4j.featureflag;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.featureflag.event.FeatureFlagEvent;

/**
 * A Feature Flag helps selectively enable/disable functionality at runtime.
 */
public interface FeatureFlag {
    /**
     * Checks if a feature is enabled.
     *
     * @param featureName the name of the feature to check
     * @return true if the feature is enabled, false otherwise
     */
    boolean isEnabled(String featureName);

    /**
     * Enables a feature.
     *
     * @param featureName the name of the feature to enable
     */
    void enable(String featureName);

    /**
     * Disables a feature.
     *
     * @param featureName the name of the feature to disable
     */
    void disable(String featureName);

    /**
     * Get the name of this feature flag instance
     *
     * @return the name of this feature flag
     */
    String getName();

    /**
     * Creates a feature flag with default configuration.
     *
     * @param name the name of the feature flag
     * @return a feature flag with default configuration
     */
    static FeatureFlag ofDefaults(String name) {
        return new DefaultFeatureFlag(name);
    }

    /**
     * Returns the EventPublisher to which all feature flag events are published
     *
     * @return the EventPublisher
     */
    EventPublisher getEventPublisher();

    /**
     * Interface to publish feature flag events
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<FeatureFlagEvent> {
        void onStateTransition(EventConsumer<FeatureFlagEvent> eventConsumer);
    }
}