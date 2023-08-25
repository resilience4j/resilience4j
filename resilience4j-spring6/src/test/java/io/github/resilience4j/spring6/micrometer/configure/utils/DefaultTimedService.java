package io.github.resilience4j.spring6.micrometer.configure.utils;

import io.github.resilience4j.micrometer.annotation.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedStage;
import static java.util.concurrent.CompletableFuture.failedFuture;

@Component
public class DefaultTimedService {

    public static final String BASIC_OPERATION_TIMER_NAME = "Basic operation";
    public static final String COMPLETABLE_STAGE_TIMER_NAME = "Completable stage";

    @Timer(name = BASIC_OPERATION_TIMER_NAME)
    public String succeed(int number) {
        return String.valueOf(number);
    }

    @Timer(name = BASIC_OPERATION_TIMER_NAME)
    public void fail() {
        throw new IllegalStateException();
    }

    @Timer(name = BASIC_OPERATION_TIMER_NAME, fallbackMethod = "fallback")
    public String recover(int number) {
        throw new IllegalStateException("Basic operation recovered " + number);
    }

    @Timer(name = "#root.args[0]", fallbackMethod = "${missing.property:fallback}")
    public String recover(String timerName, int number) {
        throw new IllegalStateException("Basic operation recovered " + number);
    }

    @Timer(name = COMPLETABLE_STAGE_TIMER_NAME)
    public CompletionStage<String> succeedCompletionStage(int number) {
        return completedStage(String.valueOf(number));
    }

    @Timer(name = COMPLETABLE_STAGE_TIMER_NAME)
    public CompletionStage<Void> failCompletionStage() {
        return failedFuture(new IllegalStateException());
    }

    @Timer(name = COMPLETABLE_STAGE_TIMER_NAME, fallbackMethod = "fallbackCompletionStage")
    public CompletionStage<String> recoverCompletionStage(int number) {
        return failedFuture(new IllegalStateException("Completable stage recovered " + number));
    }

    @Timer(name = "#root.args[0]", fallbackMethod = "${missing.property:fallbackCompletionStage}")
    public CompletionStage<String> recoverCompletionStage(String timerName, int number) {
        return failedFuture(new IllegalStateException("Completable stage recovered " + number));
    }

    private String fallback(IllegalStateException e) {
        return e.getMessage();
    }

    private CompletionStage<String> fallbackCompletionStage(IllegalStateException e) {
        return completedStage(e.getMessage());
    }
}
