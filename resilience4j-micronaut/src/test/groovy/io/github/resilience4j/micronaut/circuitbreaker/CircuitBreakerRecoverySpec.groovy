/*
 * Copyright 2020 Michael Pollind
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
package io.github.resilience4j.micronaut.circuitbreaker

import io.github.resilience4j.annotation.CircuitBreaker
import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.circuitbreaker.enabled", value = "true")
class CircuitBreakerRecoverySpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    @Inject
    CircuitBreakerService service;

    void "test async recovery circuit breaker"() {
        when:
        CompletableFuture<String> response = service.asynRecoverable();

        then:
        response.get() == "recovered"
    }


    void "test sync recovery circuit breaker"() {
        when:
        String response = service.syncRecoverable()

        then:
        response == "recovered"
    }

    @Singleton
    static class CircuitBreakerService extends TestDummyService {
        @CircuitBreaker(name = "backend-a", fallbackMethod = 'completionStageRecovery')
        CompletableFuture<String> asynRecoverable() {
            return asyncError();
        }

        @CircuitBreaker(name = "backend-a", fallbackMethod = 'syncRecovery')
        String syncRecoverable() {
            return syncError();
        }
    }
}
