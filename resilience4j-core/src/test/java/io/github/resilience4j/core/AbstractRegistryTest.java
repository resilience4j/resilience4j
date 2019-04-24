package io.github.resilience4j.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;


public class AbstractRegistryTest {

	private Logger LOGGER;

	@Before
	public void setUp() {
		LOGGER = mock(Logger.class);
	}

	@Test
	public void testAbstractRegistryActions() {
		Consumer<String> consumer1 = circuitBreaker -> LOGGER.info("invoking the post consumer1");
		TestRegistry testRegistry = new TestRegistry();
		testRegistry.registerPostCreationConsumer(consumer1);
		testRegistry.addConfiguration("test", "test");
		assertThat(testRegistry.getConfiguration("test").get()).isEqualTo("test");
		assertThat(testRegistry.getDefaultConfig()).isEqualTo("default");
		testRegistry.notifyPostCreationConsumers("test");
		then(LOGGER).should(times(1)).info("invoking the post consumer1");
		testRegistry.unregisterPostCreationConsumer(consumer1);
		testRegistry.notifyPostCreationConsumers("test");
		then(LOGGER).shouldHaveZeroInteractions();
	}

	class TestRegistry extends AbstractRegistry<String, String> {

		public TestRegistry() {
			super();
			String defaults = "default";
			this.configurations.put(DEFAULT_CONFIG, defaults);

		}
	}

}