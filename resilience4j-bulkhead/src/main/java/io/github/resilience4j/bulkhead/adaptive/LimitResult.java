package io.github.resilience4j.bulkhead.adaptive;

import io.github.resilience4j.core.metrics.Snapshot;

import java.util.Objects;

/**
 * the adapted new limit result that will be calculated through the configured {@link LimitPolicy#adaptLimitIfAny(Snapshot, int)}
 */
public class LimitResult {

	final int limit;
	final long waitTime;

	public LimitResult(int limit, long waitTime) {
		this.limit = limit;
		this.waitTime = waitTime;
	}

	/**
	 * @return the the adapted bulkhead limit based into the configured {@link LimitPolicy} calculation
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * @return the the adapted bulkhead wait time based into the configured {@link LimitPolicy} calculation
	 */
	public long waitTime() {
		return waitTime;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LimitResult that = (LimitResult) o;
		return limit == that.limit &&
				waitTime == that.waitTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(limit, waitTime);
	}

	@Override
	public String toString() {
		return "LimitResult{" +
				"limit=" + limit +
				", waitTime=" + waitTime +
				'}';
	}


}
