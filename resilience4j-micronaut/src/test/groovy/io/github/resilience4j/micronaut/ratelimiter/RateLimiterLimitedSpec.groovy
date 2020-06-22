package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.annotation.RateLimiter
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.ratelimiter.enabled", value = "true")
@Property(name = "resilience4j.ratelimiter.instances.low.limitForPeriod", value = "1")
@Property(name = "resilience4j.ratelimiter.instances.low.limitRefreshPeriod", value = "PT10s")
class RateLimiterLimitedSpec extends Specification{
    @Inject ApplicationContext applicationContext

    @Inject @Client("/ratelimiter-limited") HttpClient client

    void "test ratelimit"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/limited", String.class)

        then:
        noExceptionThrown()
        response.body() == "ok"

        when:
        client.toBlocking().exchange("/limited", String.class)

        then:
        def ex = thrown(HttpClientResponseException)
        ex.response.code() == HttpStatus.INTERNAL_SERVER_ERROR.code
        ex.message == "Internal Server Error: RateLimiter 'low' does not permit further calls"

        // test rate limit on another function
        when:
        client.toBlocking().exchange("/limited2", String.class)

        then:
        noExceptionThrown()
        response.body() == "ok"

        when:
        client.toBlocking().exchange("/limited2", String.class)

        then:
        ex = thrown(HttpClientResponseException)
        ex.response.code() == HttpStatus.INTERNAL_SERVER_ERROR.code
        ex.message == "Internal Server Error: RateLimiter 'low' does not permit further calls"
    }

    void "test unlimited"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange("/unlimited", String.class)

        then:
        noExceptionThrown()
        response.body() == "ok"

        when:
        response = client.toBlocking().exchange("/unlimited", String.class)

        then:
        noExceptionThrown()
        response.body() == "ok"

        when:
        response = client.toBlocking().exchange("/unlimited", String.class)

        then:
        noExceptionThrown()
        response.body() == "ok"
    }

    @Controller("/ratelimiter-limited")
    static class RateLimitedController {
        @RateLimiter(name = "low")
        @Get("/limited")
        String limited() {
            return "ok"
        }

        @RateLimiter(name = "low")
        @Get("/limited2")
        String limited2() {
            return "ok"
        }

        @Get("/unlimited")
        String unLimited() {
            return "ok"
        }
    }
}
