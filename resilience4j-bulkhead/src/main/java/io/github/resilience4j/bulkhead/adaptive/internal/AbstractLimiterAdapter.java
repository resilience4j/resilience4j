package io.github.resilience4j.bulkhead.adaptive.internal;

import static java.lang.Math.max;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitAdapter;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitDecreasedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitIncreasedEvent;

/**
 * abstract limiter logic
 */
public abstract class AbstractLimiterAdapter implements LimitAdapter<Bulkhead> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractLimiterAdapter.class);
	protected static final double NANO_SCALE = 1_000_000_000d;
	protected static final double MILLI_SCALE = 1_000_000d;
	private final double concurrencyDropMul;
	protected volatile double currentMaxLatency;
	private final Consumer<BulkheadLimit> publishEventConsumer;

	protected AbstractLimiterAdapter(double concurrency_drop_mul, double currentMaxLatency, Consumer<BulkheadLimit> publishEventConsumer) {
		concurrencyDropMul = concurrency_drop_mul;
		this.currentMaxLatency = currentMaxLatency;
		this.publishEventConsumer = publishEventConsumer;
	}


	@Override
	public double getMaxLatencyMillis() {
		return currentMaxLatency * 1000;
	}

	@Override
	public Consumer<BulkheadLimit> bulkheadLimitConsumer() {
		return publishEventConsumer;
	}

	/**
	 * adopt the limit based into the new calculated average
	 *
	 * @param bulkhead              the target semaphore bulkhead
	 * @param averageLatencySeconds calculated average latency
	 * @param waitTimeMillis        new wait time
	 */
	protected void adoptLimit(Bulkhead bulkhead, double averageLatencySeconds, long waitTimeMillis) {
		if (averageLatencySeconds < currentMaxLatency) {
			final int newMaxConcurrentCalls = bulkhead.getBulkheadConfig().getMaxConcurrentCalls() + 1;
			if (LOG.isDebugEnabled()) {
				LOG.debug("increasing bulkhead limit by increasing the max concurrent calls for {}", newMaxConcurrentCalls);
			}
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(newMaxConcurrentCalls)
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
			bulkheadLimitConsumer().accept(new BulkheadOnLimitIncreasedEvent(bulkhead.getName().substring(0, bulkhead.getName().indexOf('-')),
					eventData(averageLatencySeconds, waitTimeMillis, newMaxConcurrentCalls)));
		} else {
			final int newMaxConcurrentCalls = max((int) (bulkhead.getBulkheadConfig().getMaxConcurrentCalls() * concurrencyDropMul), 1);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Dropping the bulkhead limit with new max concurrent calls {}", newMaxConcurrentCalls);
			}
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(newMaxConcurrentCalls)
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
			bulkheadLimitConsumer().accept(new BulkheadOnLimitDecreasedEvent(bulkhead.getName().substring(0, bulkhead.getName().indexOf('-')),
					eventData(averageLatencySeconds, waitTimeMillis, newMaxConcurrentCalls)));
		}
	}


	private Map<String, String> eventData(double averageLatencySeconds, long waitTimeMillis, int newMaxConcurrentCalls) {
		Map<String, String> eventData = new HashMap<>();
		eventData.put("averageLatencySeconds", String.valueOf(averageLatencySeconds));
		eventData.put("newMaxConcurrentCalls", String.valueOf(newMaxConcurrentCalls));
		eventData.put("newWaitTimeMillis", String.valueOf(waitTimeMillis));
		eventData.put("currentMaxLatency", String.valueOf(currentMaxLatency));
		return eventData;
	}
}
