package io.github.resilience4j.micronaut.bulkhead;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import spock.lang.Specification;

@MicronautTest
@Property(name = "resilience4j.thread-pool-bulkhead.enabled", value = "true")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.maxThreadPoolSize", value = "10")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.coreThreadPoolSize", value = "5")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.queueCapacity", value = "4")
@Property(name = "resilience4j.bulkhead.configs.default.keepAliveDuration", value = "PT10S")
@Property(name = "resilience4j.thread-pool-bulkhead.instances.backendA.baseConfig", value = "default")
public class ThreadPoolBulkheadSpec extends Specification {
}
