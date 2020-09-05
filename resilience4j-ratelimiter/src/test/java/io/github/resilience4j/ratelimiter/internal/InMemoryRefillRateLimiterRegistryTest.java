/*
 *
 *  Copyright 2020 Emmanouil Gkatziouras
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
package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.core.registry.*;
import io.github.resilience4j.ratelimiter.*;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class InMemoryRefillRateLimiterRegistryTest {

    private static final int LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REFRESH_PERIOD = Duration.ofNanos(500);
    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    @Rule
    public ExpectedException exception = ExpectedException.none();
    private RefillRateLimiterConfig config;

    @Before
    public void init() {
        config = RefillRateLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .limitRefreshPeriod(REFRESH_PERIOD)
            .limitForPeriod(LIMIT)
            .build();
    }

    @Test
    public void rateLimiterPositive() throws Exception {
        RefillRateLimiterRegistry registry = RefillRateLimiterRegistry.of(config);
        RateLimiter firstRateLimiter = registry.rateLimiter("test");
        RateLimiter anotherLimit = registry.rateLimiter("test1");
        RateLimiter sameAsFirst = registry.rateLimiter("test");

        then(firstRateLimiter).isEqualTo(sameAsFirst);
        then(firstRateLimiter).isNotEqualTo(anotherLimit);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rateLimiterPositiveWithSupplier() throws Exception {
        RefillRateLimiterRegistry registry = new InMemoryRefillRateLimiterRegistry(config);
        Supplier<RefillRateLimiterConfig> rateLimiterConfigSupplier = mock(Supplier.class);
        when(rateLimiterConfigSupplier.get())
            .thenReturn(config);

        RateLimiter firstRateLimiter = registry.rateLimiter("test", rateLimiterConfigSupplier);
        verify(rateLimiterConfigSupplier, times(1)).get();
        RateLimiter sameAsFirst = registry.rateLimiter("test", rateLimiterConfigSupplier);
        verify(rateLimiterConfigSupplier, times(1)).get();
        RateLimiter anotherLimit = registry.rateLimiter("test1", rateLimiterConfigSupplier);
        verify(rateLimiterConfigSupplier, times(2)).get();

        then(firstRateLimiter).isEqualTo(sameAsFirst);
        then(firstRateLimiter).isNotEqualTo(anotherLimit);
    }

    @Test
    public void rateLimiterConfigIsNull() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);
        new InMemoryRefillRateLimiterRegistry((RefillRateLimiterConfig) null);
    }

    @Test
    public void rateLimiterNewWithNullName() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        RefillRateLimiterRegistry registry = new InMemoryRefillRateLimiterRegistry(config);
        registry.rateLimiter(null);
    }

    @Test
    public void rateLimiterNewWithNullNonDefaultConfig() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);
        RefillRateLimiterRegistry registry = new InMemoryRefillRateLimiterRegistry(config);
        RefillRateLimiterConfig rateLimiterConfig = null;
        registry.rateLimiter("name", rateLimiterConfig);
    }

    @Test
    public void rateLimiterNewWithNullNameAndNonDefaultConfig() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        RefillRateLimiterRegistry registry = new InMemoryRefillRateLimiterRegistry(config);
        registry.rateLimiter(null, config);
    }

    @Test
    public void rateLimiterNewWithNullNameAndConfigSupplier() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        RefillRateLimiterRegistry registry = new InMemoryRefillRateLimiterRegistry(config);
        registry.rateLimiter(null, () -> config);
    }

    @Test
    public void rateLimiterNewWithNullConfigSupplier() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage("Supplier must not be null");
        RefillRateLimiterRegistry registry = new InMemoryRefillRateLimiterRegistry(config);
        Supplier<RefillRateLimiterConfig> rateLimiterConfigSupplier = null;
        registry.rateLimiter("name", rateLimiterConfigSupplier);
    }

    @Test
    public void rateLimiterGetAllRateLimiters() {
        RefillRateLimiterRegistry registry = new InMemoryRefillRateLimiterRegistry(config);
        registry.rateLimiter("foo");

        assertThat(registry.getAllRateLimiters().size()).isEqualTo(1);
        assertThat(registry.getAllRateLimiters().get(0).getName()).isEqualTo("foo");
    }

    @Test
    public void shouldCreateRateLimiterRegistryWithRegistryStore() {
        RegistryEventConsumer<RateLimiter> registryEventConsumer = getNoOpsRegistryEventConsumer();
        List<RegistryEventConsumer<RateLimiter>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(registryEventConsumer);
        Map<String, RefillRateLimiterConfig> configs = new HashMap<>();
        final RefillRateLimiterConfig defaultConfig = RefillRateLimiterConfig.ofDefaults();
        configs.put("default", defaultConfig);
        final RefillRateLimiterRegistry inMemoryRateLimiterRegistry =
            new InMemoryRefillRateLimiterRegistry(configs, registryEventConsumers,
                io.vavr.collection.HashMap.of("Tag1", "Tag1Value"), new InMemoryRegistryStore());

        AssertionsForClassTypes.assertThat(inMemoryRateLimiterRegistry).isNotNull();
        AssertionsForClassTypes.assertThat(inMemoryRateLimiterRegistry.getDefaultConfig()).isEqualTo(defaultConfig);
        AssertionsForClassTypes.assertThat(inMemoryRateLimiterRegistry.getConfiguration("testNotFound")).isEmpty();
        inMemoryRateLimiterRegistry.addConfiguration("testConfig", defaultConfig);
        AssertionsForClassTypes.assertThat(inMemoryRateLimiterRegistry.getConfiguration("testConfig")).isNotNull();
    }

    private RegistryEventConsumer<RateLimiter> getNoOpsRegistryEventConsumer() {
        return new RegistryEventConsumer<RateLimiter>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<RateLimiter> entryAddedEvent) {
            }
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<RateLimiter> entryRemoveEvent) {
            }
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<RateLimiter> entryReplacedEvent) {
            }
        };
    }

}
