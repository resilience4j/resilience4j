package io.github.resilience4j.micronaut

import io.github.resilience4j.micronaut.annotation.Retry
import io.micronaut.context.annotation.Executable
import io.micronaut.context.annotation.Property
import io.micronaut.retry.exception.FallbackException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.Flowable
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

@MicronautTest
@Property(name = "resilience4j.retry.enabled", value = "true")
class FallbackSpec extends Specification{
    @Inject
    FallbackService service;

    void "fallback completable with null return"() {
        setup:
        CompletableFuture<String> result =  service.completableNull()

        when:
        result.get()

        then:
        ExecutionException ex = thrown()
        Exception inner = ex.getCause();
        inner instanceof FallbackException
        inner.getMessage() == "Fallback handler [CompletableFuture recoveryCompletableNull()] returned null value"
    }

    void "fallback flowable with null return"() {
        setup:
        Flowable<String> result =  service.flowableNull()

        when:
        result.blockingFirst()

        then:
        FallbackException ex = thrown()
        ex instanceof FallbackException
        ex.getMessage() == "Fallback handler [CompletableFuture recoveryFlowableNull()] returned null value"
    }


    @Singleton
    static class FallbackService {
        @Retry(name = "default", fallbackMethod = 'recoveryCompletableNull')
        CompletableFuture<String> completableNull() {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Test"));
            return future
        }

        @Executable
        CompletableFuture<String> recoveryCompletableNull() {
            return null;
        }

        @Retry(name = "default", fallbackMethod = 'recoveryFlowableNull')
        Flowable<String> flowableNull() {
            return Flowable.error(new RuntimeException("Test"));
        }

        @Executable
        CompletableFuture<String> recoveryFlowableNull() {
            return null;
        }
    }
}
