/*
 *
 *  Copyright 2019 authors
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.function.Supplier;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class InMemoryTimeLimiterRegistryTest {

    private static final String CONFIG_MUST_NOT_BE_NULL = "Config must not be null";
    private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";
    @Rule
    public ExpectedException exception = ExpectedException.none();
    private TimeLimiterConfig config;

    @Before
    public void init() {
        config = TimeLimiterConfig.ofDefaults();
    }

    @Test
    public void timeLimiterPositive() {
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);

        TimeLimiter firstTimeLimiter = registry.timeLimiter("test");
        TimeLimiter anotherLimit = registry.timeLimiter("test1");
        TimeLimiter sameAsFirst = registry.timeLimiter("test");

        then(firstTimeLimiter).isEqualTo(sameAsFirst);
        then(firstTimeLimiter).isNotEqualTo(anotherLimit);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void timeLimiterPositiveWithSupplier() {
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);
        Supplier<TimeLimiterConfig> timeLimiterConfigSupplier = mock(Supplier.class);
        given(timeLimiterConfigSupplier.get()).willReturn(config);

        TimeLimiter firstTimeLimiter = registry.timeLimiter("test", timeLimiterConfigSupplier);
        verify(timeLimiterConfigSupplier, times(1)).get();
        TimeLimiter sameAsFirst = registry.timeLimiter("test", timeLimiterConfigSupplier);
        verify(timeLimiterConfigSupplier, times(1)).get();
        TimeLimiter anotherLimit = registry.timeLimiter("test1", timeLimiterConfigSupplier);
        verify(timeLimiterConfigSupplier, times(2)).get();

        then(firstTimeLimiter).isEqualTo(sameAsFirst);
        then(firstTimeLimiter).isNotEqualTo(anotherLimit);
    }

    @Test
    public void timeLimiterConfigIsNull() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);

        new InMemoryTimeLimiterRegistry((TimeLimiterConfig) null);
    }

    @Test
    public void timeLimiterNewWithNullName() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);

        registry.timeLimiter(null);
    }

    @Test
    public void timeLimiterNewWithNullNonDefaultConfig() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(CONFIG_MUST_NOT_BE_NULL);
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);

        registry.timeLimiter("name", (TimeLimiterConfig) null);
    }

    @Test
    public void timeLimiterNewWithNullNameAndNonDefaultConfig() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);

        registry.timeLimiter(null, config);
    }

    @Test
    public void timeLimiterNewWithNullNameAndConfigSupplier() {
        exception.expect(NullPointerException.class);
        exception.expectMessage(NAME_MUST_NOT_BE_NULL);
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);

        registry.timeLimiter(null, () -> config);
    }

    @Test
    public void timeLimiterNewWithNullConfigSupplier() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("Supplier must not be null");
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);

        registry.timeLimiter("name", (Supplier<TimeLimiterConfig>) null);
    }

    @Test
    public void timeLimiterGetAllTimeLimiters() {
        TimeLimiterRegistry registry = new InMemoryTimeLimiterRegistry(config);

        final TimeLimiter timeLimiter = registry.timeLimiter("foo");

        then(registry.getAllTimeLimiters().size()).isEqualTo(1);
        then(registry.getAllTimeLimiters()).contains(timeLimiter);
    }

}