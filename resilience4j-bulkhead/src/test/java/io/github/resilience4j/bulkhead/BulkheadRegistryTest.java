/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.registerCustomDateFormat;
import static org.assertj.core.api.BDDAssertions.assertThat;


public class BulkheadRegistryTest {

    private BulkheadConfig config;
    private BulkheadRegistry registry;

    @Before
    public void setUp() {

        // registry with default config
        registry = BulkheadRegistry.ofDefaults();

        // registry with custom config
        config = BulkheadConfig.custom()
                               .maxConcurrentCalls(100)
                               .maxWaitTime(50)
                               .build();
    }

    @Test
    public void shouldReturnCustomConfig() {

        // give
        BulkheadRegistry registry = BulkheadRegistry.of(config);

        // when
        BulkheadConfig bulkheadConfig = registry.getDefaultBulkheadConfig();

        // then
        assertThat(bulkheadConfig).isSameAs(config);
    }

    @Test
    public void shouldReturnTheCorrectName() {

        Bulkhead bulkhead = registry.bulkhead("test");

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getName()).isEqualTo("test");
        assertThat(bulkhead.getMaxConcurrentCalls()).isEqualTo(25);
        assertThat(bulkhead.getAvailableConcurrentCalls()).isEqualTo(25);
    }

    @Test
    public void shouldBeTheSameInstance() {

        Bulkhead bulkhead1 = registry.bulkhead("test", config);
        Bulkhead bulkhead2 = registry.bulkhead("test", config);

        assertThat(bulkhead1).isSameAs(bulkhead2);
        assertThat(registry.getAllBulkheads()).hasSize(1);
    }

    @Test
    public void shouldBeNotTheSameInstance() {

        Bulkhead bulkhead1 = registry.bulkhead("test1");
        Bulkhead bulkhead2 = registry.bulkhead("test2");

        assertThat(bulkhead1).isNotSameAs(bulkhead2);
        assertThat(registry.getAllBulkheads()).hasSize(2);
    }

}
