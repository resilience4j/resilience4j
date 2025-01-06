package io.github.resilience4j.featureflag.event;

import io.github.resilience4j.core.Event;

/**
 * An event which is created by a FeatureFlag.
 */
public interface FeatureFlagEvent extends Event {

    /**
     * Returns the feature name.
     *
     * @return the feature name
     */
    String getFeatureName();

    /**
     * Events types that are created by a FeatureFlag.
     */
    enum Type implements Event.Type {
        /**
         * Event fired when a feature is enabled
         */
        FEATURE_ENABLED,
        
        /**
         * Event fired when a feature is disabled
         */
        FEATURE_DISABLED
    }
}