package io.github.resilience4j.bulkhead.adaptive.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadRegistry;

/**
 * @author romeh
 */
public class InMemoryAdaptiveBulkheadRegistryTest {

    private AdaptiveBulkheadConfig config;
	private AdaptiveBulkheadRegistry registry;

    @Before
    public void setUp() {
        // registry with default config
        registry = AdaptiveBulkheadRegistry.ofDefaults();
        // registry with custom config
        config = AdaptiveBulkheadConfig.custom()
            .maxConcurrentCalls(300)
            .slowCallDurationThreshold(Duration.ofMillis(1))
            .build();
    }

	@Test
	public void shouldReturnCustomConfig() {
		// give
		AdaptiveBulkheadRegistry registry = AdaptiveBulkheadRegistry.of(config);
		// when
		AdaptiveBulkheadConfig bulkheadConfig = registry.getDefaultConfig();
		// then
		assertThat(bulkheadConfig).isSameAs(config);
	}

	@Test
	public void shouldReturnTheCorrectName() {
		AdaptiveBulkhead bulkhead = registry.bulkhead("test");
		assertThat(bulkhead).isNotNull();
		assertThat(bulkhead.getName()).isEqualTo("test");
	}

	@Test
	public void shouldBeTheSameInstance() {
		AdaptiveBulkhead bulkhead1 = registry.bulkhead("test", config);
		AdaptiveBulkhead bulkhead2 = registry.bulkhead("test", config);

		assertThat(bulkhead1).isSameAs(bulkhead2);
		assertThat(registry.getAllBulkheads()).hasSize(1);
	}

	@Test
	public void shouldBeNotTheSameInstance() {
		AdaptiveBulkhead bulkhead1 = registry.bulkhead("test1");
		AdaptiveBulkhead bulkhead2 = registry.bulkhead("test2");

		assertThat(bulkhead1).isNotSameAs(bulkhead2);
		assertThat(registry.getAllBulkheads()).hasSize(2);
	}

	@Test
	public void testCreateWithConfigurationMap() {
		Map<String, AdaptiveBulkheadConfig> configs = new HashMap<>();
		configs.put("default", AdaptiveBulkheadConfig.ofDefaults());
		configs.put("custom", AdaptiveBulkheadConfig.ofDefaults());

		AdaptiveBulkheadRegistry bulkheadRegistry = AdaptiveBulkheadRegistry.of(configs);

		assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
		assertThat(bulkheadRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testCreateWithConfigurationMapWithoutDefaultConfig() {
		Map<String, AdaptiveBulkheadConfig> configs = new HashMap<>();
		configs.put("custom", AdaptiveBulkheadConfig.ofDefaults());

		AdaptiveBulkheadRegistry bulkheadRegistry = AdaptiveBulkheadRegistry.of(configs);

		assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
		assertThat(bulkheadRegistry.getConfiguration("custom")).isNotNull();
	}

	@Test
	public void testAddConfiguration() {
		AdaptiveBulkheadRegistry bulkheadRegistry = AdaptiveBulkheadRegistry.ofDefaults();
		bulkheadRegistry.addConfiguration("custom", AdaptiveBulkheadConfig.ofDefaults());

		assertThat(bulkheadRegistry.getDefaultConfig()).isNotNull();
		assertThat(bulkheadRegistry.getConfiguration("custom")).isNotNull();
	}

}