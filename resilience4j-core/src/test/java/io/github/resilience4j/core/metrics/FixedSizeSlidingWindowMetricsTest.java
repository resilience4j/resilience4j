/*
 *
 *  Copyright 2019 Robert Winkler and Bohdan Storozhuk
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

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class FixedSizeSlidingWindowMetricsTest {

    private List<Metrics> defaultSlidingWindow() {
        return Arrays.asList(
                new FixedSizeSlidingWindowMetrics(5),
                new LockFreeFixedSizeSlidingWindowMetrics(5)
        );
    }

    private List<Metrics> mediumSizeSlidingWindow() {
        return Arrays.asList(
                new FixedSizeSlidingWindowMetrics(4),
                new LockFreeFixedSizeSlidingWindowMetrics(4)
        );
    }

    private List<Metrics> smallSizeSlidingWindow() {
        return Arrays.asList(
                new FixedSizeSlidingWindowMetrics(2),
                new LockFreeFixedSizeSlidingWindowMetrics(2)
        );
    }

    @Test
    @Parameters(method = "defaultSlidingWindow")
    public void checkInitialBucketCreation(Metrics metrics) {
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

    @Test
    @Parameters(method = "defaultSlidingWindow")
    public void testRecordSuccess(Metrics metrics) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isZero();
    }

    @Test
    @Parameters(method = "defaultSlidingWindow")
    public void testRecordError(Metrics metrics) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(100);
    }

    @Test
    @Parameters(method = "defaultSlidingWindow")
    public void testRecordSlowSuccess(Metrics metrics) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isZero();
    }

    @Test
    @Parameters(method = "defaultSlidingWindow")
    public void testSlowCallsPercentage(Metrics metrics) {
        metrics.record(10000, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_SUCCESS);
        metrics.record(10000, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_ERROR);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);

        Snapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(5);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(20300);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(4060);
        assertThat(snapshot.getSlowCallRate()).isEqualTo(40f);
    }

    @Test
    public void testMoveHeadIndexByOne() {
        FixedSizeSlidingWindowMetrics metrics = new FixedSizeSlidingWindowMetrics(3);

        assertThat(metrics.headIndex).isZero();

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isEqualTo(1);

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isEqualTo(2);

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isZero();

        metrics.moveHeadIndexByOne();

        assertThat(metrics.headIndex).isEqualTo(1);

    }

    @Test
    @Parameters(method = "mediumSizeSlidingWindow")
    public void testSlidingWindowMetrics(Metrics metrics) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(100);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(200);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(50);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(3);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(300);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(33.333332f);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);

        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(4);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(3);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(400);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(25);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);

        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(4);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(4);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(400);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isZero();
    }

    @Test
    @Parameters(method = "smallSizeSlidingWindow")
    public void testSlidingWindowMetricsWithSlowCalls(Metrics metrics) {
        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_ERROR);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(100);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SLOW_SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isEqualTo(1);
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(200);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isEqualTo(50);

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isEqualTo(1);
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(200);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isZero();

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isZero();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(200);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isZero();

        snapshot = metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
        assertThat(snapshot.getTotalNumberOfCalls()).isEqualTo(2);
        assertThat(snapshot.getTotalNumberOfSlowCalls()).isZero();
        assertThat(snapshot.getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(snapshot.getNumberOfFailedCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowSuccessfulCalls()).isZero();
        assertThat(snapshot.getNumberOfSlowFailedCalls()).isZero();
        assertThat(snapshot.getTotalDuration().toMillis()).isEqualTo(200);
        assertThat(snapshot.getAverageDuration().toMillis()).isEqualTo(100);
        assertThat(snapshot.getFailureRate()).isZero();
    }
}
