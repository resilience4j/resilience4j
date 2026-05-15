/*
 *
 *  Copyright 2026 authors
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
package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InMemoryTimeLimiterRegistryTest {

    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    private TimeLimiterConfig config;

    @BeforeEach
    void init() {
        config = TimeLimiterConfig.ofDefaults();
    }

    @Test
    void timeLimiterPositive() {
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);

        TimeLimiter firstTimeLimiter = registry.timeLimiter("test");
        TimeLimiter anotherLimit = registry.timeLimiter("test1");
        TimeLimiter sameAsFirst = registry.timeLimiter("test");

        then(firstTimeLimiter).isEqualTo(sameAsFirst);
        then(firstTimeLimiter).isNotEqualTo(anotherLimit);
    }

    @Test
    @SuppressWarnings("unchecked")
    void timeLimiterPositiveWithSupplier() {
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);
        Supplier<TimeLimiterConfig> timeLimiterConfigSupplier = mock(Supplier.class);
        given(timeLimiterConfigSupplier.get()).willReturn(config);

        TimeLimiter firstTimeLimiter = registry.timeLimiter("test", timeLimiterConfigSupplier);
        verify(timeLimiterConfigSupplier).get();
        TimeLimiter sameAsFirst = registry.timeLimiter("test", timeLimiterConfigSupplier);
        verify(timeLimiterConfigSupplier).get();
        TimeLimiter anotherLimit = registry.timeLimiter("test1", timeLimiterConfigSupplier);
        verify(timeLimiterConfigSupplier, times(2)).get();

        then(firstTimeLimiter).isEqualTo(sameAsFirst);
        then(firstTimeLimiter).isNotEqualTo(anotherLimit);
    }

    @Test
    void timeLimiterConfigIsNull() {
        assertThatThrownBy(() -> new InMemoryTimeLimiterRegistry((TimeLimiterConfig) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(CONFIG_MUST_NOT_BE_NULL);
    }

    @Test
    void timeLimiterNewWithNullName() {
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);
        assertThatThrownBy(() -> registry.timeLimiter(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(NAME_MUST_NOT_BE_NULL);
    }

    @Test
    void timeLimiterNewWithNullNonDefaultConfig() {
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);
        assertThatThrownBy(() -> registry.timeLimiter("name", (TimeLimiterConfig) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(CONFIG_MUST_NOT_BE_NULL);
    }

    @Test
    void timeLimiterNewWithNullNameAndNonDefaultConfig() {
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);
        assertThatThrownBy(() -> registry.timeLimiter(null, config))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(NAME_MUST_NOT_BE_NULL);
    }

    @Test
    void timeLimiterNewWithNullNameAndConfigSupplier() {
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);
        assertThatThrownBy(() -> registry.timeLimiter(null, () -> config))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(NAME_MUST_NOT_BE_NULL);
    }

    @Test
    void timeLimiterNewWithNullConfigSupplier() {
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);
        assertThatThrownBy(() -> registry.timeLimiter("name", (Supplier<TimeLimiterConfig>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Supplier must not be null");
    }

    @Test
    void timeLimiterGetAllTimeLimiters() {
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);

        final TimeLimiter timeLimiter = registry.timeLimiter("foo");

        then(registry.getAllTimeLimiters()).hasSize(1);
        then(registry.getAllTimeLimiters()).contains(timeLimiter);
    }
}
