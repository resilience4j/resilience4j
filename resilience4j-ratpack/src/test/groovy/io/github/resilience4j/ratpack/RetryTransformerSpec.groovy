package io.github.resilience4j.ratpack

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import ratpack.exec.Blocking
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class RetryTransformerSpec extends Specification {


    def "no retry without exception"() {
        given:
        Retry retry = buildRetry()
        RetryTransformer<String> transformer = RetryTransformer.of(retry)
        AtomicInteger times = new AtomicInteger(0)

        when:
        def r = ExecHarness.yieldSingle {
            Blocking.<String> get { times.getAndIncrement(); "s" }
                    .transform(transformer)
        }

        then:
        r.value == "s"
        !r.error
        r.throwable == null
        times.get() == 1
    }

    def "can retry promise 3 times then throw exception"() {
        given:
        Retry retry = buildRetry()
        RetryTransformer<String> transformer = RetryTransformer.of(retry)
        Exception e = new Exception("puke")
        AtomicInteger times = new AtomicInteger(0)

        when:
        def r = ExecHarness.yieldSingle {
            Blocking.<String> get { times.getAndIncrement(); throw e }
                    .transform(transformer)
        }

        then:
        r.value == null
        r.error
        r.throwable == e
        times.get() == 3
    }

    def "can retry promise 3 times then recover"() {
        given:
        Retry retry = buildRetry()
        RetryTransformer<String> transformer = RetryTransformer.of(retry).recover { t -> "bar" }
        Exception e = new Exception("puke")
        AtomicInteger times = new AtomicInteger(0)

        when:
        def r = ExecHarness.yieldSingle {
            Blocking.<String> get { times.getAndIncrement(); throw e }
                    .transform(transformer)
        }

        then:
        r.value == "bar"
        !r.error
        r.throwable == null
        times.get() == 3
    }

    def buildRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build()
        Retry.of("test", config)
    }
}
