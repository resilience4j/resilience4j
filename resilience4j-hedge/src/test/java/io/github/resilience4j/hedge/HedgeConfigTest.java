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

import io.github.resilience4j.hedge.metrics.AveragePlusMetrics;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.BDDAssertions.then;

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
            .preconfiguredMetrics(null);
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
    public void testBuilderCreatesPercentMetrics() {
        HedgeConfig config = HedgeConfig.custom()
            .averagePlusPercentMetrics(100, false).build();

        then(((AveragePlusMetrics) config.newMetrics())).isInstanceOf(AveragePlusMetrics.class);
    }

    @Test
    public void shouldCreateAmountMetrics() {
        HedgeConfig config = HedgeConfig.custom()
            .averagePlusAmountMetrics(200, false, 100).build();

        HedgeMetrics metrics = config.newMetrics();

        then(((AveragePlusMetrics) metrics)).isInstanceOf(AveragePlusMetrics.class);
        then(((AveragePlusMetrics) metrics).getFactor()).isEqualTo(200);
        then(((AveragePlusMetrics) metrics).shouldMeasureErrors()).isEqualTo(false);
        then(((AveragePlusMetrics) metrics).shouldUseFactorAsPercentage()).isEqualTo(false);
    }


    @Test
    public void shouldUseProvidedExecutor() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        HedgeConfig config = HedgeConfig.custom()
            .withProvidedExecutor(service)
            .build();

        then(config.getHedgeExecutor()).isEqualTo(service);
    }

    @Test
    public void shouldUseConfiguredExecutor() {
        HedgeConfig config = HedgeConfig.custom()
            .withConfiguredExecutor(10, "TEST", new ThreadPoolExecutor.DiscardPolicy())
            .build();

        then(((ScheduledThreadPoolExecutor) config
            .getHedgeExecutor())
            .getThreadFactory()
            .newThread(() -> {
            })
            .getName())
            .isEqualTo("hedge-TEST-1");
    }
}
