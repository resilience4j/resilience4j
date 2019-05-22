package io.github.resilience4j.bulkhead.configure.threadpool;

/**
 * common spring configuration properties fir {@link io.github.resilience4j.bulkhead.ThreadPoolBulkhead}
 */
public class ThreadPoolProperties {

	private int maxThreadPoolSize;
	private int coreThreadPoolSize;
	private int queueCapacity;
	private long keepAliveTime;

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
