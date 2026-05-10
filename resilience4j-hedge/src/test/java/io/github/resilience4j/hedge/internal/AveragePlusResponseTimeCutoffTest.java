/*
 *
 *  Copyright 2026: Matthew Sandoz
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
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.hedge.event.HedgeEvent;

import org.junit.jupiter.api.Test;


import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

class AveragePlusResponseTimeCutoffTest {

    private static final Duration LONG_DURATION = Duration.ofMillis(1000);
    private static final Duration SHORT_DURATION = Duration.ofMillis(100);

    @Test
    void shouldComputeProperlyFromSuccesses() {
        AverageDurationSupplier supplier = (AverageDurationSupplier) HedgeDurationSupplier.ofAveragePlus(true, 200, false, 100);

        supplier.accept(HedgeEvent.Type.PRIMARY_SUCCESS, LONG_DURATION);
        supplier.accept(HedgeEvent.Type.PRIMARY_SUCCESS, SHORT_DURATION);

        then(supplier.get()).isEqualTo(Duration.ofMillis(1100));
    }

    @Test
    void shouldNotComputeFromHedges() {
        AverageDurationSupplier supplier = (AverageDurationSupplier) HedgeDurationSupplier.ofAveragePlus(false, 0, false, 100);

        supplier.accept(HedgeEvent.Type.SECONDARY_SUCCESS, LONG_DURATION);

        then(supplier.get()).isEqualTo(Duration.ofMillis(0));
    }

    @Test
    void shouldNotComputeFromErrors() {
        AverageDurationSupplier supplier = (AverageDurationSupplier) HedgeDurationSupplier.ofAveragePlus(false, 0, false, 100);

        supplier.accept(HedgeEvent.Type.PRIMARY_FAILURE, LONG_DURATION);

        then(supplier.get()).isEqualTo(Duration.ofMillis(0));
    }

    @Test
    void shouldComputeFromErrorsIfConfigured() {
        AverageDurationSupplier supplier = (AverageDurationSupplier) HedgeDurationSupplier.ofAveragePlus(false, 0, true, 100);

        supplier.accept(HedgeEvent.Type.PRIMARY_SUCCESS, SHORT_DURATION);
        supplier.accept(HedgeEvent.Type.PRIMARY_FAILURE, LONG_DURATION);

        then(supplier.get()).isEqualTo(Duration.ofMillis(550));
    }

    @Test
    void shouldUseCorrectAdditiveFactor() {
        AverageDurationSupplier supplier = (AverageDurationSupplier) HedgeDurationSupplier.ofAveragePlus(false, 100, false, 100);

        supplier.accept(HedgeEvent.Type.PRIMARY_SUCCESS, LONG_DURATION);

        then(supplier.get()).isEqualTo(Duration.ofMillis(1100));
    }

}