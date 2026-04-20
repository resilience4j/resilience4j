/*
 *
 *  Copyright 2026: Matthew Sandoz
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
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.HedgeRegistry;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;


import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InMemoryHedgeRegistryTest {

    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private HedgeConfig config;

    @BeforeEach
    void init() {
        config = HedgeConfig.ofDefaults();
    }

    @Test
    void hedgePositive() {
        HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();

        Hedge firstHedge = registry.hedge("test");
        Hedge anotherLimit = registry.hedge("test1");
        Hedge sameAsFirst = registry.hedge("test");

        then(firstHedge).isEqualTo(sameAsFirst);
        then(firstHedge).isNotEqualTo(anotherLimit);
    }

    @Test
    @SuppressWarnings("unchecked")
    void hedgePositiveWithSupplier() {
        HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();
        Supplier<HedgeConfig> hedgeConfigSupplier = mock(Supplier.class);
        given(hedgeConfigSupplier.get()).willReturn(config);

        Hedge firstHedge = registry.hedge("test", hedgeConfigSupplier);
        verify(hedgeConfigSupplier).get();
        Hedge sameAsFirst = registry.hedge("test", hedgeConfigSupplier);
        verify(hedgeConfigSupplier).get();
        Hedge anotherLimit = registry.hedge("test1", hedgeConfigSupplier);
        verify(hedgeConfigSupplier, times(2)).get();

        then(firstHedge).isEqualTo(sameAsFirst);
        then(firstHedge).isNotEqualTo(anotherLimit);
    }

    @Test
    void hedgeConfigIsNull() {
        assertThatThrownBy(() -> HedgeRegistry.builder().withDefaultConfig(null).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining(CONFIG_MUST_NOT_BE_NULL);
    }

    @Test
    void hedgeNewWithNullName() {
        HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();
        assertThatThrownBy(() -> registry.hedge(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining(NAME_MUST_NOT_BE_NULL);
    }

    @Test
    void hedgeNewWithNullNonDefaultConfig() {
        HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();
        assertThatThrownBy(() -> registry.hedge("name", (HedgeConfig) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining(CONFIG_MUST_NOT_BE_NULL);
    }

    @Test
    void hedgeNewWithNullNameAndNonDefaultConfig() {
        HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();
        assertThatThrownBy(() -> registry.hedge(null, config))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining(NAME_MUST_NOT_BE_NULL);
    }

    @Test
    void hedgeNewWithNullNameAndConfigSupplier() {
        HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();
        assertThatThrownBy(() -> registry.hedge(null, () -> config))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining(NAME_MUST_NOT_BE_NULL);
    }

    @Test
    void hedgeNewWithNullConfigSupplier() {
        HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();
        assertThatThrownBy(() -> registry.hedge("name", (Supplier<HedgeConfig>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Supplier must not be null");
    }

    @Test
    void hedgeGetAllHedges() {
        HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();

        registry.hedge("foo");

        then(registry.getAllHedges().count()).isOne();
        then(registry.getAllHedges().findFirst().orElseThrow().getName()).isEqualTo("foo");
    }

}