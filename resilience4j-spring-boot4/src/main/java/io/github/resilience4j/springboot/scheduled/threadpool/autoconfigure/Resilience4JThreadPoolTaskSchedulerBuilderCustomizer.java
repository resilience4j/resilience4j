package io.github.resilience4j.springboot.scheduled.threadpool.autoconfigure;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;

public interface Resilience4JThreadPoolTaskSchedulerBuilderCustomizer {
    @NonNull ThreadPoolTaskSchedulerBuilder customize(@NonNull ThreadPoolTaskSchedulerBuilder builder);
}
