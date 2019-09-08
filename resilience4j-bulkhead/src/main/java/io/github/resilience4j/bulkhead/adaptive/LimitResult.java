package io.github.resilience4j.bulkhead.adaptive;

import java.util.Objects;

/**
 * limit result DTO
 */
public class LimitResult {

	final int limit;
	final long waitTime;

	public LimitResult(int limit, long waitTime) {
		this.limit = limit;
		this.waitTime = waitTime;
	}

	public int getLimit() {
		return limit;
	}

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
