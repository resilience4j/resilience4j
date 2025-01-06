package io.github.resilience4j.featureflag.event;

/**
 * Event emitted when a feature is enabled
 */
public class FeatureFlagOnEvent extends AbstractFeatureFlagEvent {

    public FeatureFlagOnEvent(String featureFlagName, String featureName) {
        super(featureFlagName, featureName, Type.FEATURE_ENABLED);
    }
}