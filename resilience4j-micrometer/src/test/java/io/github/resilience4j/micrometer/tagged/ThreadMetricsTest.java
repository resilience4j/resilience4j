/*
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.core.ThreadType;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThreadMetrics}.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
class ThreadMetricsTest {

    private MeterRegistry meterRegistry;
    private static final String SYSTEM_PROPERTY_KEY = "resilience4j.thread.type";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(SYSTEM_PROPERTY_KEY);
    }

    @Test
    void shouldRegisterMetrics() {
        ThreadMetrics threadMetrics = ThreadMetrics.ofMeterRegistry(meterRegistry);

        assertThat(meterRegistry.find(ThreadMetrics.DEFAULT_PREFIX + ".virtual_thread_enabled").gauge())
            .isNotNull();
    }

    @Test
    void shouldReportVirtualThreadsDisabled() {
        System.clearProperty(SYSTEM_PROPERTY_KEY);

        ThreadMetrics.ofMeterRegistry(meterRegistry);

        Gauge gauge = meterRegistry.find(ThreadMetrics.DEFAULT_PREFIX + ".virtual_thread_enabled").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isZero();
    }

    @Test
    void shouldReportVirtualThreadsEnabled() {
        System.setProperty(SYSTEM_PROPERTY_KEY, ThreadType.VIRTUAL.toString());

        ThreadMetrics.ofMeterRegistry(meterRegistry);

        Gauge gauge = meterRegistry.find(ThreadMetrics.DEFAULT_PREFIX + ".virtual_thread_enabled").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(1.0);
    }

    @Test
    void shouldUseCustomPrefix() {
        String customPrefix = "custom.prefix";
        ThreadMetrics.ofMeterRegistry(customPrefix, meterRegistry);

        assertThat(meterRegistry.find(customPrefix + ".virtual_thread_enabled").gauge())
            .isNotNull();
    }
}
