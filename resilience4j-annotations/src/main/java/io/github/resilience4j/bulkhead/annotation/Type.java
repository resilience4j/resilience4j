package io.github.resilience4j.bulkhead.annotation;

/**
 * bulkhead implementation types
 * <p>
 * SEMAPHORE will invoke semaphore based bulkhead implementation
 * THREADPOOL will invoke Thread pool based bulkhead implementation
 */
public enum Type {
	SEMAPHORE, THREADPOOL
}
