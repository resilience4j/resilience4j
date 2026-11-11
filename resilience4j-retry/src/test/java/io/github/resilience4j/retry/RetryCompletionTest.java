package io.github.resilience4j.retry;

import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.retry.event.RetryEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryCompletionTest {

    @ParameterizedTest
    @EnumSource(Decorator.class)
    void shouldRecordExhaustedResultOnlyOnce(Decorator decorator) {
        assertExhaustedResult(decorator, RetryConfig.custom());
    }

    @ParameterizedTest
    @EnumSource(Decorator.class)
    void shouldNotApplyExceptionPredicateToCompletionFailure(Decorator decorator) {
        assertExhaustedResult(decorator, RetryConfig.custom()
            .retryOnException(exception -> !(exception instanceof MaxRetriesExceededException)));
    }

    @ParameterizedTest
    @EnumSource(Decorator.class)
    void shouldNotRetrySuccessfulCallWhenCompletionConsumerThrows(Decorator decorator) {
        Retry retry = Retry.of("test", RetryConfig.custom()
            .maxAttempts(3).waitDuration(Duration.ZERO).build());
        AtomicInteger invocations = new AtomicInteger();
        IllegalStateException consumerFailure = new IllegalStateException("completion consumer");
        retry.getEventPublisher().onSuccess(event -> { throw consumerFailure; });

        assertThatThrownBy(() -> decorator.execute(retry, () -> {
            if (invocations.incrementAndGet() == 1) {
                throw new IllegalArgumentException("retry the call");
            }
            return "success";
        })).isSameAs(consumerFailure);

        assertThat(invocations).hasValue(2);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isZero();
    }

    private void assertExhaustedResult(Decorator decorator, RetryConfig.Builder<Object> builder) {
        Retry retry = Retry.of("test", builder.maxAttempts(3)
            .waitDuration(Duration.ZERO)
            .retryOnResult(result -> true)
            .failAfterMaxAttempts(true).build());
        AtomicInteger invocations = new AtomicInteger();
        List<RetryEvent> events = new ArrayList<>();
        retry.getEventPublisher().onEvent(events::add);

        assertThatThrownBy(() -> decorator.execute(retry, () -> {
            invocations.incrementAndGet();
            return "retry";
        })).isInstanceOf(MaxRetriesExceededException.class);

        assertThat(invocations).hasValue(3);
        assertThat(events).extracting(RetryEvent::getEventType)
            .containsExactly(RetryEvent.Type.RETRY, RetryEvent.Type.RETRY, RetryEvent.Type.ERROR);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt()).isZero();
    }

    enum Decorator {
        SUPPLIER, CHECKED_SUPPLIER, CALLABLE, FUNCTION, CHECKED_FUNCTION,
        RUNNABLE, CHECKED_RUNNABLE, CONSUMER, CHECKED_CONSUMER;

        void execute(Retry retry, CheckedSupplier<String> supplier) throws Throwable {
            switch (this) {
                case SUPPLIER -> Retry.decorateSupplier(retry, () -> unchecked(supplier)).get();
                case CHECKED_SUPPLIER -> Retry.decorateCheckedSupplier(retry, supplier).get();
                case CALLABLE -> Retry.decorateCallable(retry, () -> unchecked(supplier)).call();
                case FUNCTION -> Retry.decorateFunction(retry, value -> unchecked(supplier)).apply(0);
                case CHECKED_FUNCTION -> Retry.decorateCheckedFunction(retry, value -> supplier.get()).apply(0);
                case RUNNABLE -> Retry.decorateRunnable(retry, () -> unchecked(supplier)).run();
                case CHECKED_RUNNABLE -> Retry.decorateCheckedRunnable(retry, () -> supplier.get()).run();
                case CONSUMER -> Retry.decorateConsumer(retry, value -> unchecked(supplier)).accept(0);
                case CHECKED_CONSUMER -> Retry.decorateCheckedConsumer(retry, value -> supplier.get()).accept(0);
            }
        }

        private static String unchecked(CheckedSupplier<String> supplier) {
            try {
                return supplier.get();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Throwable throwable) {
                throw new AssertionError(throwable);
            }
        }
    }
}
