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

import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class HedgeImplTest {

    private static final String NAME = "name";
    private final HedgeConfig hedgeConfig = HedgeConfig.custom().preconfiguredDuration(Duration.ZERO).build();
    @Mock
    private final HedgeEventProcessor eventProcessor = new HedgeEventProcessor();
    @InjectMocks
    private final Hedge hedge = new io.github.resilience4j.hedge.internal.HedgeImpl("name", hedgeConfig);

    @Test
    void shouldSetGivenName() {
        Hedge hedge = Hedge.ofDefaults(NAME);
        assertThat(hedge.getName()).isEqualTo(NAME);
    }

    @Test
    void shouldPropagateConfig() {
        then(hedge.getHedgeConfig()).isEqualTo(hedgeConfig);
    }

    @Test
    void shouldPropagateName() {
        then(hedge.getName()).isEqualTo(NAME);
    }

    @Test
    void shouldDefaultToPreconfiguredSupplier() {
        then(hedge.getDurationSupplier()).isOfAnyClassIn(PreconfiguredDurationSupplier.class);
    }

    @Test
    void shouldHandleConsumerErrors() {
        hedge.getEventPublisher().onEvent(event -> {
            throw new RuntimeException("BAD_CONSUMER");
        });
        assertThatNoException().isThrownBy(() -> hedge.onPrimarySuccess(Duration.ofMillis(1000)));
    }

    @Test
    void shouldNotPublishWithoutConsumers() {
        assertThatNoException().isThrownBy(() -> hedge.onPrimarySuccess(Duration.ofMillis(1000)));
        assertThatNoException().isThrownBy(() -> hedge.onSecondarySuccess(Duration.ofMillis(1000)));
        assertThatNoException().isThrownBy(() -> hedge.onPrimaryFailure(Duration.ofMillis(1000), new Throwable()));
        assertThatNoException().isThrownBy(() -> hedge.onSecondaryFailure(Duration.ofMillis(1000), new Throwable()));

        Mockito.verify(eventProcessor, Mockito.never()).consumeEvent(any());
    }
}
