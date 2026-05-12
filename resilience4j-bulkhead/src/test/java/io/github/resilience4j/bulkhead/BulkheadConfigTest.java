/*
 *
 *  Copyright 2026 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BulkheadConfigTest {

    @Test
    void buildCustomWithDuration() {
        int maxConcurrent = 66;
        long maxWait = 555;

        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrent)
            .maxWaitDuration(Duration.ofMillis(555))
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(maxConcurrent);
        Assertions.assertThat(config.getMaxWaitDuration()).hasMillis(maxWait);
    }

    @Test
    void buildCustomWithWritableStackTraceDisabled() {
        int maxConcurrent = 66;

        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrent)
            .writableStackTraceEnabled(false)
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(maxConcurrent);
        assertThat(config.isWritableStackTraceEnabled()).isFalse();
    }

    @Test
    void buildCustomWithFairStrategyDisabled() {

        BulkheadConfig config = BulkheadConfig.custom()
            .fairCallHandlingStrategyEnabled(false)
            .build();

        assertThat(config).isNotNull();
        assertThat(config.isFairCallHandlingEnabled()).isFalse();
    }

    @Test
    void buildCustom() {
        int maxConcurrent = 66;
        long maxWait = 555;

        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrent)
            .maxWaitDuration(Duration.ofMillis(maxWait))
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(maxConcurrent);
        Assertions.assertThat(config.getMaxWaitDuration()).hasMillis(maxWait);
        assertThat(config.isFairCallHandlingEnabled()).isTrue();
    }

    @Test
    void buildWithZeroMaxCurrentCalls() {
        int maxConcurrent = 0;

        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrent)
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(maxConcurrent);
    }

    @Test
    void buildWithIllegalMaxConcurrent() {
        assertThatThrownBy(() -> BulkheadConfig.custom().maxConcurrentCalls(-1).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildWithIllegalMaxWait() {
        assertThatThrownBy(() -> BulkheadConfig.custom().maxWaitDuration(Duration.ofMillis(-1)).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildWithIllegalMaxWaitDuration() {
        assertThatThrownBy(() -> BulkheadConfig.custom().maxWaitDuration(Duration.ofSeconds(-1)).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testToString() {
        int maxConcurrent = 66;
        long maxWait = 555;

        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(maxConcurrent)
            .maxWaitDuration(Duration.ofMillis(maxWait))
            .writableStackTraceEnabled(false)
            .fairCallHandlingStrategyEnabled(false)
            .build();

        String result = config.toString();
        assertThat(result)
                .startsWith("BulkheadConfig{")
                .contains("maxConcurrentCalls=66")
                .contains("maxWaitDuration=PT0.555S")
                .contains("writableStackTraceEnabled=false")
                .contains("fairCallHandlingEnabled=false")
                .endsWith("}");
    }
}
