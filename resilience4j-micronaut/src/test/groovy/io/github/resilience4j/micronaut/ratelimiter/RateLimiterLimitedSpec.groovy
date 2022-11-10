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

import io.github.resilience4j.micronaut.annotation.RateLimiter
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification


@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
@Property(name = "resilience4j.ratelimiter.instances.low.limitForPeriod", value = "1")
@Property(name = "resilience4j.ratelimiter.instances.low.limitRefreshPeriod", value = "PT10s")
@Property(name = "resilience4j.ratelimiter.instances.low.subscribeForEvents", value = "true")
class RateLimiterLimitedSpec extends Specification{
    @Inject ApplicationContext applicationContext

    @Inject
    RateLimitedService service

    def "test service with rate limited service cap"() {
        when:
        String result = service.limited()

        then:
        noExceptionThrown()
        result == "ok"

        when:
        service.limited()

        then:
        def ex = thrown(RequestNotPermitted)
        ex.message == "RateLimiter 'low' does not permit further calls"

        // test rate limit on another function
        when:
        String result2 = service.limited()

        then:
        noExceptionThrown()
        result2 == "ok"

        when:
        service.limited()

        then:
        ex = thrown(RequestNotPermitted)
        ex.message == "RateLimiter 'low' does not permit further calls"
    }

    def "test service with unlimited service cap"() {
        when:
        String result1 = service.unLimited()

        then:
        noExceptionThrown()
        result1 == "ok"

        when:
        String result2 = service.unLimited()

        then:
        noExceptionThrown()
        result2 == "ok"

        when:
        String result3 = service.unLimited()

        then:
        noExceptionThrown()
        result3 == "ok"
    }

    @Singleton
    static class RateLimitedService {
        @RateLimiter(name = "low")
        String limited() {
            return "ok"
        }

        @RateLimiter(name = "low")
        String limited2() {
            return "ok"
        }

        String unLimited() {
            return "ok"
        }
    }
}
