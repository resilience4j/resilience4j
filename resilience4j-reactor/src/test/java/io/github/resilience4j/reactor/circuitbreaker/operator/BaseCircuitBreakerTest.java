/*
 * Copyright 2018 Julien Hoarau
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
package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.test.HelloWorldService;
import org.junit.Before;
import org.mockito.Mockito;

/**
 * Helper class to test and assert circuit breakers.
 */
class BaseCircuitBreakerTest {

    CircuitBreaker circuitBreaker;
    HelloWorldService helloWorldService;

    @Before
    public void setUp(){
        circuitBreaker = Mockito.mock(CircuitBreaker.class);
        circuitBreaker = Mockito.mock(CircuitBreaker.class);
    }
}
