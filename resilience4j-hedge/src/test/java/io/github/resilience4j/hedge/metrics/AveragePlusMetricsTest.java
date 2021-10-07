/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge.metrics;

import io.github.resilience4j.hedge.HedgeMetrics;
import io.github.resilience4j.hedge.event.HedgeEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

public class AveragePlusMetricsTest {

    private static final Duration LONG_DURATION = Duration.ofMillis(1000);
    private static final Duration SHORT_DURATION = Duration.ofMillis(100);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldComputeProperlyFromSuccesses() {
        AveragePlusMetrics metrics = (AveragePlusMetrics) HedgeMetrics.ofAveragePlus(true, 200, false, 100);

        metrics.accept(HedgeEvent.Type.PRIMARY_SUCCESS, LONG_DURATION);
        metrics.accept(HedgeEvent.Type.PRIMARY_SUCCESS, SHORT_DURATION);

        then(metrics.getResponseTimeCutoff()).isEqualTo(Duration.ofMillis(1100));
    }

    @Test
    public void shouldNotComputeFromHedges() {
        AveragePlusMetrics metrics = (AveragePlusMetrics) HedgeMetrics.ofAveragePlus(false, 0, false, 100);

        metrics.accept(HedgeEvent.Type.HEDGE_SUCCESS, LONG_DURATION);

        then(metrics.getResponseTimeCutoff()).isEqualTo(Duration.ofMillis(0));
    }

    @Test
    public void shouldNotComputeFromErrors() {
        AveragePlusMetrics metrics = (AveragePlusMetrics) HedgeMetrics.ofAveragePlus(false, 0, false, 100);

        metrics.accept(HedgeEvent.Type.PRIMARY_FAILURE, LONG_DURATION);

        then(metrics.getResponseTimeCutoff()).isEqualTo(Duration.ofMillis(0));
    }

    @Test
    public void shouldComputeFromErrorsIfConfigured() {
        AveragePlusMetrics metrics = (AveragePlusMetrics) HedgeMetrics.ofAveragePlus(false, 0, true, 100);

        metrics.accept(HedgeEvent.Type.PRIMARY_SUCCESS, SHORT_DURATION);
        metrics.accept(HedgeEvent.Type.PRIMARY_FAILURE, LONG_DURATION);

        then(metrics.getResponseTimeCutoff()).isEqualTo(Duration.ofMillis(550));
    }

    @Test
    public void shouldUseCorrectAdditiveFactor() {
        AveragePlusMetrics metrics = (AveragePlusMetrics) HedgeMetrics.ofAveragePlus(false, 100, false, 100);

        metrics.accept(HedgeEvent.Type.PRIMARY_SUCCESS, LONG_DURATION);

        then(metrics.getResponseTimeCutoff()).isEqualTo(Duration.ofMillis(1100));
    }

}