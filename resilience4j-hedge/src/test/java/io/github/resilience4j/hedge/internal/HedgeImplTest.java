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
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.metrics.PreconfiguredCutoffMetrics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;

public class HedgeImplTest {

    private static final String NAME = "name";
    private final HedgeConfig hedgeConfig = HedgeConfig.custom().preconfiguredMetrics(Duration.ZERO).build();
    @Mock
    private final HedgeEventProcessor eventProcessor = new HedgeEventProcessor();
    @InjectMocks
    private final Hedge hedge = new io.github.resilience4j.hedge.internal.HedgeImpl("name", hedgeConfig);

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldSetGivenName() {
        Hedge hedge = Hedge.ofDefaults(NAME);
        assertThat(hedge.getName()).isEqualTo(NAME);
    }

    @Test
    public void shouldPropagateConfig() {
        then(hedge.getHedgeConfig()).isEqualTo(hedgeConfig);
    }

    @Test
    public void shouldPropagateName() {
        then(hedge.getName()).isEqualTo(NAME);
    }

    @Test
    public void shouldDefaultMetrics() {
        then(hedge.getMetrics()).isOfAnyClassIn(PreconfiguredCutoffMetrics.class);
    }

    @Test
    public void shouldHandleConsumerErrors() {
        hedge.getEventPublisher().onEvent(event -> {
            throw new RuntimeException("BAD_CONSUMER");
        });
        hedge.onPrimarySuccess(Duration.ofMillis(1000));
    }

    @Test
    public void shouldNotPublishWithoutConsumers() {
        Mockito.doThrow(new RuntimeException("fail")).when(eventProcessor).consumeEvent(any());
        hedge.onPrimarySuccess(Duration.ofMillis(1000));
        hedge.onHedgedSuccess(Duration.ofMillis(1000));
        hedge.onPrimaryFailure(Duration.ofMillis(1000), new Throwable());
        hedge.onHedgedFailure(Duration.ofMillis(1000), new Throwable());
    }
}
