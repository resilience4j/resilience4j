package io.github.resilience4j.featureflag.event;

import java.time.ZonedDateTime;

/**
 * Abstract class for feature flag events
 */
abstract class AbstractFeatureFlagEvent implements FeatureFlagEvent {
    private final String featureFlagName;
    private final String featureName;
    private final Type eventType;
    private final ZonedDateTime creationTime;

    AbstractFeatureFlagEvent(String featureFlagName, String featureName, Type eventType) {
        this.featureFlagName = featureFlagName;
        this.featureName = featureName;
        this.eventType = eventType;
        this.creationTime = ZonedDateTime.now();
    }

    @Override
    public String getFeatureName() {
        return featureName;
    }

    @Override
    public Type getEventType() {
        return eventType;
    }

    @Override
    public ZonedDateTime getCreationTime() {
        return creationTime;
    }

    public String getFeatureFlagName() {
        return featureFlagName;
    }
}