package io.github.resilience4j.featureflag;

import io.github.resilience4j.featureflag.event.FeatureFlagEvent;
import io.github.resilience4j.featureflag.event.FeatureFlagOnEvent;
import io.github.resilience4j.featureflag.event.FeatureFlagOffEvent;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FeatureFlagTest {

    @Test
    public void shouldCreateFeatureFlagWithDefaults() {
        FeatureFlag featureFlag = FeatureFlag.ofDefaults("testFlag");
        assertThat(featureFlag.getName()).isEqualTo("testFlag");
        assertThat(featureFlag.isEnabled("feature1")).isFalse();
    }

    @Test
    public void shouldEnableFeature() {
        FeatureFlag featureFlag = FeatureFlag.ofDefaults("testFlag");
        featureFlag.enable("feature1");
        assertThat(featureFlag.isEnabled("feature1")).isTrue();
    }

    @Test
    public void shouldDisableFeature() {
        FeatureFlag featureFlag = FeatureFlag.ofDefaults("testFlag");
        featureFlag.enable("feature1");
        featureFlag.disable("feature1");
        assertThat(featureFlag.isEnabled("feature1")).isFalse();
    }

    @Test
    public void shouldEmitEvents() {
        FeatureFlag featureFlag = FeatureFlag.ofDefaults("testFlag");
        List<FeatureFlagEvent> events = new ArrayList<>();
        
        featureFlag.getEventPublisher().onEvent(events::add);
        
        featureFlag.enable("feature1");
        featureFlag.disable("feature1");
        
        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(FeatureFlagOnEvent.class);
        assertThat(events.get(1)).isInstanceOf(FeatureFlagOffEvent.class);
    }
}