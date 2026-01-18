package io.github.resilience4j.springboot3.thread.autoconfigure;

import io.github.resilience4j.core.ThreadType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Resilience4j thread type.
 *
 * <p>Property prefix: {@code resilience4j.thread}.</p>
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@ConfigurationProperties(prefix = "resilience4j.thread")
public class ThreadTypeProperties {

    /**
     * The thread type to be used by Resilience4j's internal schedulers.
     * Supported values:
     * <ul>
     *     <li>{@code platform} (default): traditional platform threads.</li>
     *     <li>{@code virtual}: Java virtual threads (Project Loom).</li>
     * </ul>
     */
    private ThreadType type = ThreadType.PLATFORM;

    /**
     * Get the thread type as enum.
     * 
     * @return the thread type
     */
    public ThreadType getType() {
        return type;
    }

    /**
     * Set the thread type using enum.
     * 
     * @param type the thread type to set
     */
    public void setType(ThreadType type) {
        this.type = type != null ? type : ThreadType.PLATFORM;
    }
}
