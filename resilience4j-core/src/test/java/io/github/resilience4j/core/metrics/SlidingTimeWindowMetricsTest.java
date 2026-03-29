/*
 *
 *  Copyright 2026 Robert Winkler and Bohdan Storozhuk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.core.metrics;

import com.statemachinesystems.mockclock.MockClock;
import io.github.resilience4j.core.JavaClockWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingTimeWindowMetricsTest {

    private static Stream<Arguments> defaultSlidingWindow() {
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));

        MockClock wrappableClock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        JavaClockWrapper wrappedClock = new JavaClockWrapper(wrappableClock);

        return Stream.of(
                Arguments.of(new SlidingTimeWindowMetrics(5, clock), clock),
                Arguments.of(new LockFreeSlidingTimeWindowMetrics(5, wrappedClock), wrappableClock)
        );
    }

    @Test
    void checkInitialBucketCreation() {
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        SlidingTimeWindowMetrics metrics = new SlidingTimeWindowMetrics(5, clock);

        PartialAggregation[] buckets = metrics.partialAggregations;

        long epochSecond = clock.instant().getEpochSecond();
        for (int i = 0; i < buckets.length; i++) {
            PartialAggregation bucket = buckets[i];
            assertThat(bucket.getEpochSecond()).isEqualTo(epochSecond + i);
        }

        Snapshot snapshot = metrics.getSnapshot();

        assertThat(snapshot.getTotalNumberOfCalls()).isZero();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration().toMillis()).isZero();
        assertThat(snapshot.getAverageDuration().toMillis()).isZero();
        assertThat(snapshot.getFailureRate()).isZero();
    }

    @ParameterizedTest
    @MethodSource("defaultSlidingWindow")
    void recordSuccess(Metrics metrics, MockClock clock) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration()).hasMillis(100);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isZero();
    }

    @ParameterizedTest
    @MethodSource("defaultSlidingWindow")
    void recordError(Metrics metrics, MockClock clock) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfFailedCalls()).isOne();
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration()).hasMillis(100);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(100);
    }

    @ParameterizedTest
    @MethodSource("defaultSlidingWindow")
    void recordSlowSuccess(Metrics metrics, MockClock clock) {
        Snapshot snapshot = metrics
            .record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isOne();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration()).hasMillis(100);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isZero();
    }

    @ParameterizedTest
    @MethodSource("defaultSlidingWindow")
    void slowCallsPercentage(Metrics metrics, MockClock clock) {
        metrics.record(10000, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_SUCCESS);
        metrics.record(10000, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_ERROR);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);

        Snapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(5);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(20300);
        assertThat(snapshot.getAverageDuration()).hasMillis(4060);
        assertThat(snapshot.getSlowCallRate()).isEqualTo(40f);
    }

    @Test
    void moveHeadIndexByOne() {
        MockClock clock = MockClock.at(2019, 8, 4, 12, 0, 0, ZoneId.of("UTC"));
        SlidingTimeWindowMetrics metrics = new SlidingTimeWindowMetrics(3, clock);

        assertThat(metrics.headIndex).isZero();

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isOne();

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isEqualTo(2);

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isZero();

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isOne();

    }

    @ParameterizedTest
    @MethodSource("defaultSlidingWindow")
    void shouldClearSlidingTimeWindowMetrics(Metrics metrics, MockClock clock) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(100);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(100);

        clock.advanceByMillis(100);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(200);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(50);

        clock.advanceByMillis(700);

        snapshot = metrics.record(1000, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(3);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(1200);
        assertThat(snapshot.getAverageDuration()).hasMillis(400);

        clock.advanceBySeconds(4);

        snapshot = metrics.getSnapshot();
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(3);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(1200);
        assertThat(snapshot.getAverageDuration()).hasMillis(400);

        clock.advanceBySeconds(1);

        snapshot = metrics.getSnapshot();

        assertThat(snapshot.getTotalNumberOfCalls()).isZero();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration().toMillis()).isZero();
        assertThat(snapshot.getAverageDuration().toMillis()).isZero();
        assertThat(snapshot.getFailureRate()).isZero();

        clock.advanceByMillis(100);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration()).hasMillis(100);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isZero();

        clock.advanceBySeconds(5);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration()).hasMillis(100);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isZero();
    }

    @ParameterizedTest
    @MethodSource("defaultSlidingWindow")
    void slidingTimeWindowMetrics(Metrics metrics, MockClock clock) {
        Snapshot result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);

        assertThat(result.getTotalNumberOfCalls()).isOne();
        assertThat(result.getNumberOfSuccessfulCalls()).isZero();
        assertThat(result.getNumberOfFailedCalls()).isOne();
        assertThat(result.getTotalDuration()).hasMillis(100);

        clock.advanceByMillis(100);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(result.getNumberOfSuccessfulCalls()).isOne();
        assertThat(result.getNumberOfFailedCalls()).isOne();
        assertThat(result.getTotalDuration()).hasMillis(200);

        clock.advanceByMillis(100);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(3);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(result.getNumberOfFailedCalls()).isOne();
        assertThat(result.getTotalDuration()).hasMillis(300);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(4);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(3);
        assertThat(result.getNumberOfFailedCalls()).isOne();
        assertThat(result.getTotalDuration()).hasMillis(400);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(5);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(result.getNumberOfFailedCalls()).isOne();
        assertThat(result.getTotalDuration()).hasMillis(500);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(6);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(result.getTotalDuration()).hasMillis(600);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(7);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(5);
        assertThat(result.getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(result.getTotalDuration()).hasMillis(700);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(5);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(result.getNumberOfFailedCalls()).isOne();
        assertThat(result.getTotalDuration()).hasMillis(500);

        clock.advanceBySeconds(1);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isEqualTo(5);
        assertThat(result.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(result.getNumberOfFailedCalls()).isOne();
        assertThat(result.getTotalDuration()).hasMillis(500);

        clock.advanceBySeconds(5);

        result = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(result.getTotalNumberOfCalls()).isOne();
        assertThat(result.getNumberOfSuccessfulCalls()).isOne();
        assertThat(result.getNumberOfFailedCalls()).isZero();
        assertThat(result.getTotalDuration()).hasMillis(100);
    }
}
