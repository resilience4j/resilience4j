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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class FixedSizeSlidingWindowMetricsTest {

    private static List<Metrics> defaultSlidingWindow() {
        return Arrays.asList(
                new FixedSizeSlidingWindowMetrics(5),
                new LockFreeFixedSizeSlidingWindowMetrics(5)
        );
    }

    private static List<Metrics> mediumSizeSlidingWindow() {
        return Arrays.asList(
                new FixedSizeSlidingWindowMetrics(4),
                new LockFreeFixedSizeSlidingWindowMetrics(4)
        );
    }

    private static List<Metrics> smallSizeSlidingWindow() {
        return Arrays.asList(
                new FixedSizeSlidingWindowMetrics(2),
                new LockFreeFixedSizeSlidingWindowMetrics(2)
        );
    }

    @ParameterizedTest
    @MethodSource("defaultSlidingWindow")
    void checkInitialBucketCreation(Metrics metrics) {
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
    void recordSuccess(Metrics metrics) {
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
    void recordError(Metrics metrics) {
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
    void recordSlowSuccess(Metrics metrics) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_SUCCESS);
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
    void slowCallsPercentage(Metrics metrics) {
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
        FixedSizeSlidingWindowMetrics metrics = new FixedSizeSlidingWindowMetrics(3);

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
    @MethodSource("mediumSizeSlidingWindow")
    void slidingWindowMetrics(Metrics metrics) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(100);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(100);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(200);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(50);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(3);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(300);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(33.333332f);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);

        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(4);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(3);
        assertThat(snapshot.getNumberOfFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(400);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(25);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);

        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(4);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration()).hasMillis(400);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isZero();
    }

    @ParameterizedTest
    @MethodSource("smallSizeSlidingWindow")
    void slidingWindowMetricsWithSlowCalls(Metrics metrics) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isOne();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfFailedCalls()).isOne();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(100);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(100);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfFailedCalls()).isOne();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isOne();
        assertThat(snapshot.getTotalDuration()).hasMillis(200);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(50);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isOne();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isOne();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration()).hasMillis(200);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isZero();

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isZero();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration()).hasMillis(200);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isZero();

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isZero();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration()).hasMillis(200);
        assertThat(snapshot.getAverageDuration()).hasMillis(100);
        assertThat(snapshot.getFailureRate()).isZero();
    }
}
