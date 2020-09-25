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
package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.micronaut.TestDummyService
import io.github.resilience4j.mirconaut.annotation.RateLimiter
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.CompletableFuture

@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
class RateLimiterRecoverySpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    @Inject
    RatelimiterService service;

    void "test async recovery ratelimiter"() {
        when:
        CompletableFuture<String> body = service.recoverable();

        then:
        body.get() == "recovered"
    }

    void "test sync recovery ratelimiter"() {
        when:
        String body = service.syncRecovertable();

        then:
        body == "recovered"
    }

    @Singleton
    static class RatelimiterService extends TestDummyService {
        @RateLimiter(name = "default", fallbackMethod = 'completionStageRecovery')
        CompletableFuture<String> recoverable() {
            return asyncError();
        }

        @RateLimiter(name = "default", fallbackMethod = 'syncRecovery')
        String syncRecovertable() {
            return syncError();
        }
    }
}
