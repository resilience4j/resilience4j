package io.github.resilience4j.retry;

import io.github.resilience4j.retry.event.RetryEvent;
import io.vavr.CheckedFunction0;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VavrRetryCompletionTest {

    @ParameterizedTest
    @EnumSource(Decorator.class)
    void shouldNotRepeatSuccessfulCallWhenCompletionListenerFails(Decorator decorator) {
        Retry retry = Retry.of("test", RetryConfig.custom()
            .maxAttempts(3).waitDuration(Duration.ZERO).build());
        AtomicInteger invocations = new AtomicInteger();
        IllegalStateException listenerFailure = new IllegalStateException("listener failed");
        retry.getEventPublisher().onSuccess(event -> { throw listenerFailure; });

        assertThatThrownBy(() -> decorator.execute(retry, () -> {
            if (invocations.incrementAndGet() == 1) {
                throw new IllegalArgumentException("temporary failure");
            }
            return "success";
        })).isSameAs(listenerFailure);

        assertThat(invocations).hasValue(2);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isZero();
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(Decorator.class)
    void shouldStillRetryBusinessFailures(Decorator decorator) throws Throwable {
        Retry retry = Retry.of("test", RetryConfig.custom()
            .maxAttempts(3).waitDuration(Duration.ZERO).build());
        AtomicInteger invocations = new AtomicInteger();

        decorator.execute(retry, () -> {
            if (invocations.incrementAndGet() < 3) {
                throw new IllegalArgumentException("temporary failure");
            }
            return "success";
        });

        assertThat(invocations).hasValue(3);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(3);
    }

    @ParameterizedTest
    @EnumSource(Decorator.class)
    void shouldCompleteWithoutRetry(Decorator decorator) throws Throwable {
        Retry retry = Retry.of("test", RetryConfig.custom()
            .maxAttempts(3).waitDuration(Duration.ZERO).build());
        AtomicInteger invocations = new AtomicInteger();

        decorator.execute(retry, () -> {
            invocations.incrementAndGet();
            return "success";
        });

        assertThat(invocations).hasValue(1);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfTotalCalls()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = Decorator.class, names = {"CHECKED_SUPPLIER", "CHECKED_FUNCTION"})
    void shouldRecordExhaustedResultsOnlyOnce(Decorator decorator) {
        assertExhaustedResults(decorator, false);
    }

    @ParameterizedTest
    @EnumSource(value = Decorator.class, names = {"CHECKED_SUPPLIER", "CHECKED_FUNCTION"})
    void shouldNotApplyExceptionPredicateToCompletionFailure(Decorator decorator) {
        assertExhaustedResults(decorator, true);
    }

    private void assertExhaustedResults(Decorator decorator, boolean excludeCompletionFailure) {
        RetryConfig.Builder<String> builder = RetryConfig.<String>custom()
            .maxAttempts(3).waitDuration(Duration.ZERO)
            .retryOnResult(result -> true).failAfterMaxAttempts(true);
        if (excludeCompletionFailure) {
            builder.retryOnException(error -> !(error instanceof MaxRetriesExceededException));
        }
        Retry retry = Retry.of("test", builder.build());
        AtomicInteger invocations = new AtomicInteger();
        List<RetryEvent.Type> events = new ArrayList<>();
        retry.getEventPublisher().onEvent(event -> events.add(event.getEventType()));

        assertThatThrownBy(() -> decorator.execute(retry, () -> {
            invocations.incrementAndGet();
            return "retry";
        })).isInstanceOf(MaxRetriesExceededException.class);

        assertThat(invocations).hasValue(3);
        assertThat(events).containsExactly(
            RetryEvent.Type.RETRY, RetryEvent.Type.RETRY, RetryEvent.Type.ERROR);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    enum Decorator {
        CHECKED_SUPPLIER, CHECKED_FUNCTION, CHECKED_RUNNABLE;

        void execute(Retry retry, CheckedFunction0<String> supplier) throws Throwable {
            switch (this) {
                case CHECKED_SUPPLIER -> VavrRetry.decorateCheckedSupplier(retry, supplier).apply();
                case CHECKED_FUNCTION -> VavrRetry.decorateCheckedFunction(retry,
                    ignored -> supplier.apply()).apply(0);
                case CHECKED_RUNNABLE -> VavrRetry.decorateCheckedRunnable(retry,
                    () -> supplier.apply()).run();
            }
        }
    }
}
