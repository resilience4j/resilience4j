package io.github.resilience4j.common.scheduled.threadpool.configuration;

public class Resilience4JThreadPoolConfigurationProperties {
    protected int corePoolSize;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 1) {
            throw new IllegalArgumentException(
                "corePoolSize must be a positive integer value >= 1");
        }
        this.corePoolSize = corePoolSize;
    }
}
