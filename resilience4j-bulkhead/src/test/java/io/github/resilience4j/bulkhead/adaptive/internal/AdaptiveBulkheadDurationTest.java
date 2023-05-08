package io.github.resilience4j.bulkhead.adaptive.internal;

import com.statemachinesystems.mockclock.MockClock;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.core.metrics.Snapshot;
import org.junit.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class AdaptiveBulkheadDurationTest {

    private AdaptiveBulkheadStateMachine bulkhead(Clock clock) {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .currentTimestampFunction(Clock::millis, TimeUnit.MILLISECONDS)
            .recordResult(r -> r instanceof Integer status && status >= 400)
            .build();
        return (AdaptiveBulkheadStateMachine) AdaptiveBulkhead.of("test", config, clock);
    }

    private MockClock clock() {
        return MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
    }

    @Test
    public void testOnResultDurationError() {
        MockClock clock = clock();
        AdaptiveBulkheadStateMachine bulkhead = bulkhead(clock);
        long startTime = bulkhead.getCurrentTimestamp();
        clock.advanceByMillis(2);

        bulkhead.onResult(startTime, bulkhead.getTimestampUnit(), 400);

        Snapshot snapshot = ((AdaptiveBulkheadMetrics) bulkhead.getMetrics()).getSnapshot();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDuration()).hasMillis(2);
        assertThat(snapshot.getAverageDuration()).hasMillis(2);
    }

    @Test
    public void testOnResultDurationSuccess() {
        MockClock clock = clock();
        AdaptiveBulkheadStateMachine bulkhead = bulkhead(clock);
        long startTime = bulkhead.getCurrentTimestamp();
        clock.advanceBySeconds(2);

        bulkhead.onResult(startTime, bulkhead.getTimestampUnit(), 200);

        Snapshot snapshot = ((AdaptiveBulkheadMetrics) bulkhead.getMetrics()).getSnapshot();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDuration()).hasSeconds(2);
        assertThat(snapshot.getAverageDuration()).hasSeconds(2);
    }

    @Test
    public void testOnErrorDuration() {
        Throwable failure = new Throwable();
        MockClock clock = clock();
        AdaptiveBulkheadStateMachine bulkhead = bulkhead(clock);
        long startTime = bulkhead.getCurrentTimestamp();
        Duration slowCallDurationThreshold = bulkhead.getBulkheadConfig().getSlowCallDurationThreshold();
        int seconds = (int) (slowCallDurationThreshold.getSeconds() - 1);
        clock.advanceBySeconds(seconds);

        bulkhead.onError(startTime, bulkhead.getTimestampUnit(), failure);
        bulkhead.onError(startTime, bulkhead.getTimestampUnit(), failure);

        Snapshot snapshot = ((AdaptiveBulkheadMetrics) bulkhead.getMetrics()).getSnapshot();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDuration()).hasSeconds(seconds * 2L);
        assertThat(snapshot.getAverageDuration()).hasSeconds(seconds);
    }

    @Test
    public void testOnErrorStartInPast() {
        Throwable failure = new Throwable();
        MockClock clock = clock();
        AdaptiveBulkheadStateMachine bulkhead = bulkhead(clock);
        long startTime = bulkhead.getCurrentTimestamp() - TimeUnit.SECONDS.toMillis(2);

        bulkhead.onError(startTime, bulkhead.getTimestampUnit(), failure);

        Snapshot snapshot = ((AdaptiveBulkheadMetrics) bulkhead.getMetrics()).getSnapshot();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDuration()).hasSeconds(2);
        assertThat(snapshot.getAverageDuration()).hasSeconds(2);
    }

    @Test
    public void testOnErrorDurationSlow() {
        Throwable failure = new Throwable();
        MockClock clock = clock();
        AdaptiveBulkheadStateMachine bulkhead = bulkhead(clock);
        long startTime = bulkhead.getCurrentTimestamp();
        Duration slowCallDurationThreshold = bulkhead.getBulkheadConfig().getSlowCallDurationThreshold();
        int seconds = (int) (slowCallDurationThreshold.getSeconds() + 1);
        clock.advanceBySeconds(seconds);

        bulkhead.onError(startTime, bulkhead.getTimestampUnit(), failure);
        bulkhead.onError(startTime, bulkhead.getTimestampUnit(), failure);

        Snapshot snapshot = ((AdaptiveBulkheadMetrics) bulkhead.getMetrics()).getSnapshot();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalDuration()).hasSeconds(seconds * 2L);
        assertThat(snapshot.getAverageDuration()).hasSeconds(seconds);
    }

    @Test
    public void testOnErrorDurationDays() {
        Throwable failure = new Throwable();
        MockClock clock = clock();
        AdaptiveBulkheadStateMachine bulkhead = bulkhead(clock);
        long startTime = bulkhead.getCurrentTimestamp();
        clock.advanceByDays(2);

        bulkhead.onError(startTime, bulkhead.getTimestampUnit(), failure);

        Snapshot snapshot = ((AdaptiveBulkheadMetrics) bulkhead.getMetrics()).getSnapshot();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDuration()).hasDays(2);
        assertThat(snapshot.getAverageDuration()).hasDays(2);
    }

    @Test
    public void testOnErrorDurationZero() {
        Throwable failure = new Throwable();
        MockClock clock = clock();
        AdaptiveBulkheadStateMachine bulkhead = bulkhead(clock);
        long startTime = bulkhead.getCurrentTimestamp();

        bulkhead.onError(startTime, bulkhead.getTimestampUnit(), failure);

        Snapshot snapshot = ((AdaptiveBulkheadMetrics) bulkhead.getMetrics()).getSnapshot();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDuration()).hasDays(0);
    }

    @Test
    public void testOnErrorDurationStartInFuture() {
        Throwable failure = new Throwable();
        MockClock clock = clock();
        AdaptiveBulkheadStateMachine bulkhead = bulkhead(clock);
        long startTime = bulkhead.getCurrentTimestamp();
        clock.advanceByMillis(2);

        bulkhead.onError(startTime + 600, bulkhead.getTimestampUnit(), failure);

        Snapshot snapshot = ((AdaptiveBulkheadMetrics) bulkhead.getMetrics()).getSnapshot();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDuration()).hasMillis(0);
        assertThat(snapshot.getAverageDuration()).hasMillis(0);
    }

}