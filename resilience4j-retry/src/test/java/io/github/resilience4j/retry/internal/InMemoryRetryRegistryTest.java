/*
 *
 *  Copyright 2020 KrnSaurabh
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
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.registry.*;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class InMemoryRetryRegistryTest {

    @Test
    public void shouldCreateRetryRegistryWithRegistryStore() {
        RegistryEventConsumer<Retry> registryEventConsumer = getNoOpsRegistryEventConsumer();
        List<RegistryEventConsumer<Retry>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(registryEventConsumer);
        Map<String, RetryConfig> configs = new HashMap<>();
        final RetryConfig defaultConfig = RetryConfig.ofDefaults();
        configs.put("default", defaultConfig);
        final InMemoryRetryRegistry inMemoryRetryRegistry =
            new InMemoryRetryRegistry(configs, registryEventConsumers,
                Map.of("Tag1", "Tag1Value"), new InMemoryRegistryStore<>());

        assertThat(inMemoryRetryRegistry).isNotNull();
        assertThat(inMemoryRetryRegistry.getDefaultConfig()).isEqualTo(defaultConfig);
        assertThat(inMemoryRetryRegistry.getConfiguration("testNotFound")).isEmpty();
        inMemoryRetryRegistry.addConfiguration("testConfig", defaultConfig);
        assertThat(inMemoryRetryRegistry.getConfiguration("testConfig")).isNotNull();
    }

    private RegistryEventConsumer<Retry> getNoOpsRegistryEventConsumer() {
        return new RegistryEventConsumer<Retry>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
            }
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
            }
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
            }
        };
    }
}
