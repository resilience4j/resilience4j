package io.github.resilience4j.bulkhead.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * unit testing for limit events
 */
public class BulkheadLimitTest {

	private Map<String, String> evntData = new HashMap<>();

	@Before
	public void init() {
		evntData.put("test1", "test1");
	}


	@Test
	public void testLimitIncreasedEvent() {
		BulkheadOnLimitIncreasedEvent bulkheadOnLimitIncreasedEvent = new BulkheadOnLimitIncreasedEvent("test", evntData);
		assertThat(bulkheadOnLimitIncreasedEvent.getEventType()).isEqualTo(BulkheadLimit.Type.LIMIT_INCREASED);
		assertThat(bulkheadOnLimitIncreasedEvent.getBulkheadName()).isEqualTo("test");
		assertThat(bulkheadOnLimitIncreasedEvent.toString()).contains("limit increased", "test1");

	}

	@Test
	public void testLimitDecreasedEvent() {
		BulkheadOnLimitDecreasedEvent bulkheadOnLimitDecreasedEvent = new BulkheadOnLimitDecreasedEvent("test", evntData);
		assertThat(bulkheadOnLimitDecreasedEvent.getEventType()).isEqualTo(BulkheadLimit.Type.LIMIT_DECREASED);
		assertThat(bulkheadOnLimitDecreasedEvent.getBulkheadName()).isEqualTo("test");
		assertThat(bulkheadOnLimitDecreasedEvent.toString()).contains("limit decreased", "test1");

	}

}