package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.annotation.RateLimiter
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification
import io.github.resilience4j.ratelimiter.RequestNotPermitted

import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
@Property(name = "resilience4j.ratelimiter.instances.low.limitForPeriod", value = "1")
@Property(name = "resilience4j.ratelimiter.instances.low.limitRefreshPeriod", value = "PT10s")
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
