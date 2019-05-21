package io.github.resilience4j.bulkhead.configure.threadpool;

import javax.validation.constraints.Min;

import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;

/**
 * common spring configuration properties fir {@link io.github.resilience4j.bulkhead.ThreadPoolBulkhead}
 */
public class ThreadPoolProperties {

	@Min(1)
	private int maxThreadPoolSize = ThreadPoolBulkheadConfig.DEFAULT_MAX_THREAD_POOL_SIZE;
	@Min(1)
	private int coreThreadPoolSize = ThreadPoolBulkheadConfig.DEFAULT_CORE_THREAD_POOL_SIZE;
	@Min(1)
	private int queueCapacity = ThreadPoolBulkheadConfig.DEFAULT_QUEUE_CAPACITY;
	@Min(1)
	private long keepAliveTime = ThreadPoolBulkheadConfig.DEFAULT_KEEP_ALIVE_TIME;

	public int getMaxThreadPoolSize() {
		return maxThreadPoolSize;
	}

	public void setMaxThreadPoolSize(int maxThreadPoolSize) {
		this.maxThreadPoolSize = maxThreadPoolSize;
	}

	public int getCoreThreadPoolSize() {
		return coreThreadPoolSize;
	}

	public void setCoreThreadPoolSize(int coreThreadPoolSize) {
		this.coreThreadPoolSize = coreThreadPoolSize;
	}

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public long getKeepAliveTime() {
		return keepAliveTime;
	}

	public void setKeepAliveTime(long keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
	}
	
}
