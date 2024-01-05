package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadRegistry;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author romeh
 */
public class InMemoryAdaptiveBulkheadRegistryTest {

    private AdaptiveBulkheadConfig customConfig;
	private AdaptiveBulkheadRegistry defaultRegistry;

    @Before
    public void setUp() {
        defaultRegistry = AdaptiveBulkheadRegistry.ofDefaults();
        customConfig = AdaptiveBulkheadConfig.custom()
            .maxConcurrentCalls(300)
            .slowCallDurationThreshold(Duration.ofMillis(1))
            .build();
    }

	@Test
	public void shouldReturnCustomConfig() {
		AdaptiveBulkheadRegistry registry = AdaptiveBulkheadRegistry.of(customConfig);

		AdaptiveBulkheadConfig bulkheadConfig = registry.getDefaultConfig();

		assertThat(bulkheadConfig).isSameAs(customConfig);
	}

	@Test
	public void shouldReturnTheCorrectName() {
		AdaptiveBulkhead bulkhead = defaultRegistry.bulkhead("test");

		assertThat(bulkhead).isNotNull();
		assertThat(bulkhead.getName()).isEqualTo("test");
	}

	@Test
	public void shouldBeTheSameInstance() {
		AdaptiveBulkhead bulkhead1 = defaultRegistry.bulkhead("test", customConfig);
		AdaptiveBulkhead bulkhead2 = defaultRegistry.bulkhead("test", customConfig);

		assertThat(bulkhead1).isSameAs(bulkhead2);
		assertThat(defaultRegistry.getAllBulkheads()).hasSize(1);
	}

	@Test
	public void shouldBeNotTheSameInstance() {
		AdaptiveBulkhead bulkhead1 = defaultRegistry.bulkhead("test1");
		AdaptiveBulkhead bulkhead2 = defaultRegistry.bulkhead("test2");

		assertThat(bulkhead1).isNotSameAs(bulkhead2);
		assertThat(defaultRegistry.getAllBulkheads()).hasSize(2);
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