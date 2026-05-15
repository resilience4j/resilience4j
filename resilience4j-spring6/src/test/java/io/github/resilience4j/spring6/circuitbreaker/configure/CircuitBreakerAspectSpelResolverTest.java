/*
 * Copyright 2026 Kyuhyen Hwang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.spring6.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.spring6.TestApplication;
import io.github.resilience4j.spring6.TestDummyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
class CircuitBreakerAspectSpelResolverTest {
    @Autowired
    @Qualifier("circuitBreakerDummyService")
    TestDummyService testDummyService;

    @Autowired
    CircuitBreakerRegistry registry;

    @Test
    void testSpel() {
        assertThat(registry.getAllCircuitBreakers().stream().filter(it -> it.getName().equals("SPEL_BACKEND")).findAny().isPresent()).isFalse();
        assertThat(testDummyService.spelSync("SPEL_BACKEND")).isEqualTo("recovered");
        assertThat(registry.getAllCircuitBreakers().stream().filter(it -> it.getName().equals("SPEL_BACKEND")).findAny().isPresent()).isTrue();
    }
}
