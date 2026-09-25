package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TimeLimiterSourceCompletionTest {

    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private final ScheduledFuture<?> timeoutFuture = mock(ScheduledFuture.class);
    private final TimeLimiter timeLimiter = TimeLimiter.of(Duration.ofSeconds(1));
    private final List<TimeLimiterEvent> events = new ArrayList<>();
    private final ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);

    @BeforeEach
    void setUp() {
        doReturn(timeoutFuture).when(scheduler)
            .schedule(any(Runnable.class), eq(1000L), eq(MILLISECONDS));
        timeLimiter.getEventPublisher().onEvent(events::add);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void timeoutShouldNotSkipSourceCompletionCallback(boolean sourceFails) {
        CompletableFuture<String> source = new CompletableFuture<>();
        AtomicBoolean callbackInvoked = new AtomicBoolean();
        CompletableFuture<String> suppliedStage = source.whenComplete(
            (result, error) -> callbackInvoked.set(true));
        CompletableFuture<String> limited = decorate(suppliedStage);
        RuntimeException failure = new RuntimeException("source failure");

        timeout.getValue().run();
        if (sourceFails) {
            source.completeExceptionally(failure);
        } else {
            source.complete("value");
        }

        then(callbackInvoked.get()).isTrue();
        if (sourceFails) {
            assertThatThrownBy(suppliedStage::join).hasCause(failure);
        } else {
            then(suppliedStage.join()).isEqualTo("value");
        }
        assertThatThrownBy(limited::join).hasCauseInstanceOf(TimeoutException.class);
        then(events).extracting(TimeLimiterEvent::getEventType)
            .containsExactly(TimeLimiterEvent.Type.TIMEOUT);
    }

    @Test
    void timeoutShouldNotCompleteSuppliedFuture() {
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> limited = decorate(source);

        timeout.getValue().run();

        then(source.isDone()).isFalse();
        assertThatThrownBy(limited::join).hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void sourceSuccessShouldCancelTimeoutAndPublishOneSuccess() {
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> limited = decorate(source);

        source.complete("value");
        timeout.getValue().run();

        then(limited.join()).isEqualTo("value");
        verify(timeoutFuture).cancel(false);
        then(events).extracting(TimeLimiterEvent::getEventType)
            .containsExactly(TimeLimiterEvent.Type.SUCCESS);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sourceFailureShouldPreserveErrorEventCause(boolean wrapped) {
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> limited = decorate(source);
        RuntimeException failure = new RuntimeException("source failure");
        Throwable sourceFailure = wrapped ? new ExecutionException(failure) : failure;

        source.completeExceptionally(sourceFailure);
        timeout.getValue().run();

        assertThatThrownBy(limited::join).hasCause(sourceFailure);
        then(events).extracting(TimeLimiterEvent::getEventType)
            .containsExactly(TimeLimiterEvent.Type.ERROR);
        then(((TimeLimiterOnErrorEvent) events.get(0)).getThrowable()).isSameAs(failure);
    }

    @Test
    void sourceCancellationShouldReachDecoratedStage() {
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> limited = decorate(source);

        source.cancel(false);
        timeout.getValue().run();

        assertThatThrownBy(limited::join).hasCauseInstanceOf(CancellationException.class);
        then(events).extracting(TimeLimiterEvent::getEventType)
            .containsExactly(TimeLimiterEvent.Type.ERROR);
    }

    @Test
    void shouldSupportMinimalCompletionStage() {
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> limited = decorate(source.minimalCompletionStage());

        source.complete("value");

        then(limited.join()).isEqualTo("value");
        then(events).extracting(TimeLimiterEvent::getEventType)
            .containsExactly(TimeLimiterEvent.Type.SUCCESS);
    }

    private CompletableFuture<String> decorate(CompletionStage<String> source) {
        CompletableFuture<String> limited = timeLimiter
            .decorateCompletionStage(scheduler, () -> source).get().toCompletableFuture();
        verify(scheduler).schedule(timeout.capture(), eq(1000L), eq(MILLISECONDS));
        return limited;
    }
}
