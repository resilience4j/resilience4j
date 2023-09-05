/*
 *  Copyright 2023 Mariusz Kopylec
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
 */
package io.github.resilience4j.micrometer;

import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.then;

public class TimerConfigTest {

    @Test
    public void shouldCreateDefaultTimerConfig() {
        TimerConfig config = TimerConfig.ofDefaults();

        then(config.getMetricNames()).isEqualTo("resilience4j.timer.calls");
        then(config.getOnFailureTagResolver().apply(new IllegalStateException())).isEqualTo("IllegalStateException");
    }

    @Test
    public void shouldCreateCustomTimerConfig() {
        TimerConfig config = TimerConfig.custom()
                .metricNames("resilience4j.timer.operations")
                .onFailureTagResolver(throwable -> throwable.getClass().getName())
                .build();

        then(config.getMetricNames()).isEqualTo("resilience4j.timer.operations");
        then(config.getOnFailureTagResolver().apply(new IllegalStateException())).isEqualTo("java.lang.IllegalStateException");
    }

    @Test
    public void shouldCreateTimerConfigFromPrototype() {
        TimerConfig prototype = TimerConfig.custom()
                .onFailureTagResolver(throwable -> throwable.getClass().getName())
                .build();
        TimerConfig config = TimerConfig.from(prototype)
                .metricNames("resilience4j.timer.operations")
                .onFailureTagResolver(throwable -> throwable.getClass().getSimpleName())
                .build();

        then(config.getMetricNames()).isEqualTo("resilience4j.timer.operations");
        then(config.getOnFailureTagResolver().apply(new IllegalStateException())).isEqualTo("IllegalStateException");
    }
}
