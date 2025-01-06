package io.github.resilience4j.featureflag;

import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.featureflag.event.FeatureFlagEvent;
import io.github.resilience4j.featureflag.event.FeatureFlagOnEvent;
import io.github.resilience4j.featureflag.event.FeatureFlagOffEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default FeatureFlag implementation.
 */
public class DefaultFeatureFlag implements FeatureFlag {
    private final String name;
    private final Map<String, Boolean> features;
    private final EventProcessor<FeatureFlagEvent> eventProcessor;

    /**
     * Creates a feature flag with default configuration.
     *
     * @param name the name of the feature flag
     */
    public DefaultFeatureFlag(String name) {
        this.name = name;
        this.features = new ConcurrentHashMap<>();
        this.eventProcessor = new EventProcessor<>();
    }

    @Override
    public boolean isEnabled(String featureName) {
        return features.getOrDefault(featureName, false);
    }

    @Override
    public void enable(String featureName) {
        features.put(featureName, true);
        eventProcessor.onEvent(new FeatureFlagOnEvent(name, featureName));
    }

    @Override
    public void disable(String featureName) {
        features.put(featureName, false);
        eventProcessor.onEvent(new FeatureFlagOffEvent(name, featureName));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }
}