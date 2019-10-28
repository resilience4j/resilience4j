package io.github.resilience4j.bulkhead.event;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * unit testing for limit events
 */
public class AdaptiveBulkheadEventTest {

	private Map<String, String> evntData = new HashMap<>();

	@Before
	public void init() {
		evntData.put("test1", "test1");
	}


	@Test
	public void testLimitIncreasedEvent() {
		BulkheadOnLimitIncreasedEvent bulkheadOnLimitIncreasedEvent = new BulkheadOnLimitIncreasedEvent("test", evntData);
		assertThat(bulkheadOnLimitIncreasedEvent.getEventType()).isEqualTo(AdaptiveBulkheadEvent.Type.LIMIT_INCREASED);
		assertThat(bulkheadOnLimitIncreasedEvent.getBulkheadName()).isEqualTo("test");
		assertThat(bulkheadOnLimitIncreasedEvent.toString()).contains("limit increased", "test1");

	}

	@Test
	public void testLimitDecreasedEvent() {
		BulkheadOnLimitDecreasedEvent bulkheadOnLimitDecreasedEvent = new BulkheadOnLimitDecreasedEvent("test", evntData);
		assertThat(bulkheadOnLimitDecreasedEvent.getEventType()).isEqualTo(AdaptiveBulkheadEvent.Type.LIMIT_DECREASED);
		assertThat(bulkheadOnLimitDecreasedEvent.getBulkheadName()).isEqualTo("test");
		assertThat(bulkheadOnLimitDecreasedEvent.toString()).contains("limit decreased", "test1");

	}


	@Test
	public void testLimitIgnoreEvent() {
		BulkheadOnIgnoreEvent bulkheadOnIgnoreEvent = new BulkheadOnIgnoreEvent("test", evntData);
		assertThat(bulkheadOnIgnoreEvent.getEventType()).isEqualTo(AdaptiveBulkheadEvent.Type.IGNORED_ERROR);
		assertThat(bulkheadOnIgnoreEvent.getBulkheadName()).isEqualTo("test");
		assertThat(bulkheadOnIgnoreEvent.toString()).contains("error is ignored", "test1");

	}


	@Test
	public void testLimitSuccessEvent() {
		BulkheadOnSuccessEvent bulkheadOnSuccessEvent = new BulkheadOnSuccessEvent("test", evntData);
		assertThat(bulkheadOnSuccessEvent.getEventType()).isEqualTo(AdaptiveBulkheadEvent.Type.SUCCESS);
		assertThat(bulkheadOnSuccessEvent.getBulkheadName()).isEqualTo("test");
		assertThat(bulkheadOnSuccessEvent.toString()).contains("call is succeeded", "test1");

	}


	@Test
	public void testLimitOnErrorEvent() {
		BulkheadOnErrorEvent bulkheadOnErrorEvent = new BulkheadOnErrorEvent("test", evntData);
		assertThat(bulkheadOnErrorEvent.getEventType()).isEqualTo(AdaptiveBulkheadEvent.Type.ERROR);
		assertThat(bulkheadOnErrorEvent.getBulkheadName()).isEqualTo("test");
		assertThat(bulkheadOnErrorEvent.toString()).contains("call is failed", "test1");

	}

}