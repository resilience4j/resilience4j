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
package io.github.resilience4j.hedge;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.github.resilience4j.hedge.internal.AverageDurationSupplier;
import io.github.resilience4j.hedge.internal.HedgeDurationSupplier;

public class HedgeConfigTest {

    private static final String HEDGE_DURATION_MUST_NOT_BE_NULL = "HedgeDuration must not be null";
    private static final String HEDGE_TO_STRING = "HedgeConfig{false,0,true,100,null}";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void builderTimeoutIsNull() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(HEDGE_DURATION_MUST_NOT_BE_NULL);

        HedgeConfig.Builder.fromConfig(HedgeConfig.ofDefaults())
            .preconfiguredDuration(null);
    }

    @Test
    public void shouldInitializeFromOtherConfig() {
        HedgeConfig config = HedgeConfig.custom().build();
        HedgeConfig.Builder builder = HedgeConfig.from(config);

        then(builder.build().toString()).isEqualTo("HedgeConfig{false,0,true,100,null}");
    }

    @Test
    public void configToString() {
        then(HedgeConfig.ofDefaults().toString()).isEqualTo(HEDGE_TO_STRING);
    }

    @Test
    public void shouldCreatePercentCutoff() {
        HedgeConfig config = HedgeConfig.custom()
            .averagePlusPercentageDuration(100, false).build();

        then(HedgeDurationSupplier.fromConfig(config)).isInstanceOf(AverageDurationSupplier.class);
    }

    @Test
    public void shouldCreateAmountCutoff() {
        HedgeConfig config = HedgeConfig.custom()
            .averagePlusAmountDuration(200, false, 100).build();

        HedgeDurationSupplier supplier = HedgeDurationSupplier.fromConfig(config);

        then(((AverageDurationSupplier) supplier)).isInstanceOf(AverageDurationSupplier.class);
        then(((AverageDurationSupplier) supplier).getFactor()).isEqualTo(200);
        then(((AverageDurationSupplier) supplier).shouldMeasureErrors()).isFalse();
        then(((AverageDurationSupplier) supplier).shouldUseFactorAsPercentage()).isFalse();
    }

//    @Test
//    public void shouldUseProvidedExecutor() {
//        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//        HedgeConfig config = HedgeConfig.custom()
//            .withProvidedExecutor(service)
//            .build();
//
//        then(config.getHedgeExecutor()).isEqualTo(service);
//    }

//    @Test
//    public void shouldUseConfiguredExecutor() {
//        HedgeConfig config = HedgeConfig.custom()
//            .withConfiguredExecutor(10, "TEST", new ThreadPoolExecutor.DiscardPolicy())
//            .build();
//
//        then(((ScheduledThreadPoolExecutor) config
//            .getHedgeExecutor())
//            .getThreadFactory()
//            .newThread(() -> {
//            })
//            .getName())
//            .isEqualTo("hedge-TEST-1");
//    }
}
