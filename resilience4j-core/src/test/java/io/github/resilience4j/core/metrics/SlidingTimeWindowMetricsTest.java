package io.github.resilience4j.core.metrics;

import com.statemachinesystems.mockclock.MockClock;
import org.junit.Test;

import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class SlidingTimeWindowMetricsTest {

    @Test
    public void checkInitialBucketCreation(){
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        SlidingTimeWindowMetrics metrics = new SlidingTimeWindowMetrics(5, clock);

        PartialAggregation[] buckets = metrics.partialAggregations;

        long epochSecond = clock.instant().getEpochSecond();
        for(int i = 0; i < buckets.length; i++){
            PartialAggregation bucket = buckets[i];
            assertThat(bucket.getEpochSecond()).isEqualTo(epochSecond + i);
        }

        Snapshot snapshot = metrics.getSnapshot();

        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(0);
    }

    @Test
    public void testRecordSuccess(){
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        Metrics metrics = new SlidingTimeWindowMetrics(5, clock);

        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfSlowCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRatePercentage()).isEqualTo(0);
    }

    @Test
    public void testRecordError(){
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        Metrics metrics = new SlidingTimeWindowMetrics(5, clock);

        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRatePercentage()).isEqualTo(100);
    }

    @Test
    public void testRecordSlowSuccess(){
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        Metrics metrics = new SlidingTimeWindowMetrics(5, clock);

        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfSlowCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRatePercentage()).isEqualTo(0);
    }

    @Test
    public void testSlowCallsPercentage(){

        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        Metrics metrics = new SlidingTimeWindowMetrics(5, clock);

        metrics.record(10000, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_SUCCESS);
        metrics.record(10000, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_ERROR);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);

        Snapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(5);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(snapshot.getNumberOfSlowCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(20300);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(4060);
        assertThat(snapshot.getSlowCallsPercentage()).isEqualTo(40f);
    }

    @Test
    public void testAverageThroughputPerSecond(){
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        Metrics metrics = new SlidingTimeWindowMetrics(5, clock);

        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        clock.advanceBySeconds(1);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        clock.advanceBySeconds(1);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);

        Snapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getAverageNumberOfCallsPerSecond()).isEqualTo(1.6f);

    }

    @Test
    public void testMoveHeadIndexByOne(){
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        SlidingTimeWindowMetrics metrics = new SlidingTimeWindowMetrics(3, clock);

        assertThat(metrics.headIndex).isEqualTo(0);

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isEqualTo(1);

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isEqualTo(2);

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isEqualTo(0);

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isEqualTo(1);

    }

    @Test
    public void shouldClearSlidingTimeWindowMetrics(){
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        Metrics metrics = new SlidingTimeWindowMetrics(5, clock);

        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRatePercentage()).isEqualTo(100);

        clock.advanceByMillis(100);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(200);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRatePercentage()).isEqualTo(50);

        clock.advanceByMillis(700);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(3);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(300);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(100);

        clock.advanceBySeconds(4);

        snapshot = metrics.getSnapshot();
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(3);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(300);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(100);

        clock.advanceBySeconds(1);

        snapshot = metrics.getSnapshot();

        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(0);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(0);
        assertThat(snapshot.getFailureRatePercentage()).isEqualTo(0);

        clock.advanceByMillis(100);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(snapshot.getTotalDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDurationInMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRatePercentage()).isEqualTo(0);
    }

    @Test
    public void testSlidingTimeWindowMetrics(){
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        Metrics metrics = new SlidingTimeWindowMetrics(5, clock);

        Snapshot result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);

        assertThat(result.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(100);


        clock.advanceByMillis(100);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(200);

        clock.advanceByMillis(100);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(3);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(300);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(4);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(3);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(400);


        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(5);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(500);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(6);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(600);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(7);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(5);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(700);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(5);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(500);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(5);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(500);

        clock.advanceBySeconds(5);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(result.getTotalDurationInMillis()).isEqualTo(100);
    }
}
