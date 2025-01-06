package io.github.resilience4j.featureflag.event;

/**
 * Event emitted when a feature is disabled
 */
public class FeatureFlagOffEvent extends AbstractFeatureFlagEvent {

    public FeatureFlagOffEvent(String featureFlagName, String featureName) {
        super(featureFlagName, featureName, Type.FEATURE_DISABLED);
    }
}