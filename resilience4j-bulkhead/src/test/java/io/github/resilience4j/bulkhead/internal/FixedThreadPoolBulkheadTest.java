/*
 *
 *  Copyright 2019 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.bulkhead.internal;


import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.core.registry.*;
import io.github.resilience4j.test.TestContextPropagators;
import io.github.resilience4j.test.TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.TestThreadLocalContextHolder;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.awaitility.Awaitility.matches;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.Assertions.assertThat;

public class FixedThreadPoolBulkheadTest {

    private ThreadPoolBulkhead bulkhead;
    private FixedThreadPoolBulkhead fixedThreadPoolBulkhead;

    @Before
    public void setUp() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(2)
            .coreThreadPoolSize(1)
            .queueCapacity(10)
            .contextPropagator(TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.class)
            .keepAliveDuration(Duration.ofMillis(10))
            .build();
        bulkhead = ThreadPoolBulkhead.of("test", config);
        fixedThreadPoolBulkhead = new FixedThreadPoolBulkhead("testPool", config);
    }

    @Test
    public void testSupplierThreadLocalContextPropagator() {

        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");

        CompletableFuture<Object> future = fixedThreadPoolBulkhead
            .submit(() -> TestThreadLocalContextHolder.get().orElse(null));

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue("ValueShouldCrossThreadBoundary")));
    }

    @Test
    public void testRunnableThreadLocalContextPropagator() {

        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        AtomicReference<String> reference = new AtomicReference<>();

        fixedThreadPoolBulkhead
            .submit(() -> reference.set((String) TestThreadLocalContextHolder.get().orElse(null)));

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference).hasValue("ValueShouldCrossThreadBoundary")));
    }

    @Test
    public void testToString() {
        String result = bulkhead.toString();

        assertThat(result).isEqualTo("FixedThreadPoolBulkhead 'test'");
    }

    @Test
    public void testCustomSettings() {
        assertThat(bulkhead.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(2);
        assertThat(bulkhead.getBulkheadConfig().getQueueCapacity()).isEqualTo(10);
        assertThat(bulkhead.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(1);
        assertThat(bulkhead.getBulkheadConfig().getKeepAliveDuration())
            .isEqualTo(Duration.ofMillis(10));
    }

    @Test
    public void testCreateWithDefaults() {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("test");

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getBulkheadConfig()).isNotNull();
        assertThat(bulkhead.getBulkheadConfig().getMaxThreadPoolSize())
            .isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_MAX_THREAD_POOL_SIZE);
        assertThat(bulkhead.getBulkheadConfig().getCoreThreadPoolSize())
            .isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_CORE_THREAD_POOL_SIZE);
        assertThat(bulkhead.getBulkheadConfig().getQueueCapacity())
            .isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_QUEUE_CAPACITY);
    }

    @Test
    public void shouldCreateThreadPoolBulkheadRegistryWithRegistryStore() {
        RegistryEventConsumer<ThreadPoolBulkhead> registryEventConsumer = getNoOpsRegistryEventConsumer();
        List<RegistryEventConsumer<ThreadPoolBulkhead>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(registryEventConsumer);
        Map<String, ThreadPoolBulkheadConfig> configs = new HashMap<>();
        final ThreadPoolBulkheadConfig defaultConfig = ThreadPoolBulkheadConfig.ofDefaults();
        configs.put("default", defaultConfig);
        final InMemoryThreadPoolBulkheadRegistry inMemoryThreadPoolBulkheadRegistry =
            new InMemoryThreadPoolBulkheadRegistry(configs, registryEventConsumers,
                Map.of("Tag1", "Tag1Value"), new InMemoryRegistryStore<>());

        AssertionsForClassTypes.assertThat(inMemoryThreadPoolBulkheadRegistry).isNotNull();
        AssertionsForClassTypes.assertThat(inMemoryThreadPoolBulkheadRegistry.getDefaultConfig()).isEqualTo(defaultConfig);
        AssertionsForClassTypes.assertThat(inMemoryThreadPoolBulkheadRegistry.getConfiguration("testNotFound")).isEmpty();
        inMemoryThreadPoolBulkheadRegistry.addConfiguration("testConfig", defaultConfig);
        AssertionsForClassTypes.assertThat(inMemoryThreadPoolBulkheadRegistry.getConfiguration("testConfig")).isNotNull();
    }

    private RegistryEventConsumer<ThreadPoolBulkhead> getNoOpsRegistryEventConsumer() {
        return new RegistryEventConsumer<ThreadPoolBulkhead>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<ThreadPoolBulkhead> entryAddedEvent) {
            }
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<ThreadPoolBulkhead> entryRemoveEvent) {
            }
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<ThreadPoolBulkhead> entryReplacedEvent) {
            }
        };
    }
}
