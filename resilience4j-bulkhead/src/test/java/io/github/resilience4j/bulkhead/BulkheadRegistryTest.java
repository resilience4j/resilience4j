/*
 *
 *  Copyright 2017 Lucas Lech
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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.BDDAssertions.assertThat;


public class BulkheadRegistryTest {

    private BulkheadRegistry bulkheadRegistry;

    @Before
    public void setUp(){
        bulkheadRegistry = BulkheadRegistry.create();
    }

    @Test
    public void shouldReturnTheCorrectName() {

        Bulkhead bulkhead = bulkheadRegistry.bulkhead("test", 1);

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getName()).isEqualTo("test");
        assertThat(bulkhead.getConfiguredDepth()).isEqualTo(1);
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldBeTheSameCircuitBreaker() {

        Bulkhead bulkhead1 = bulkheadRegistry.bulkhead("test", 1);
        Bulkhead bulkhead2 = bulkheadRegistry.bulkhead("test", 1);

        assertThat(bulkhead1).isSameAs(bulkhead2);
        assertThat(bulkheadRegistry.getAllBulkheads()).hasSize(1);
    }

    @Test
    public void shouldBeNotTheSameCircuitBreaker() {

        Bulkhead bulkhead1 = bulkheadRegistry.bulkhead("test1", 1);
        Bulkhead bulkhead2 = bulkheadRegistry.bulkhead("test2", 1);

        assertThat(bulkhead1).isNotSameAs(bulkhead2);
        assertThat(bulkheadRegistry.getAllBulkheads()).hasSize(2);
    }

}
