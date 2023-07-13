/*
 * Copyright 2023 Mariusz Kopylec
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
package io.github.resilience4j.reactor.micrometer.operator;

import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static io.github.resilience4j.micrometer.TimerAssertions.thenFailureTimed;
import static io.github.resilience4j.micrometer.TimerAssertions.thenSuccessTimed;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.BDDAssertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.BDDAssertions.then;

public class FluxTimerTest {

    @Test
    public void shouldTimeSuccessfulNonEmptyFlux() {
        List<String> messages = List.of("Hello 1", "Hello 2", "Hello 3");
        MeterRegistry registry = new SimpleMeterRegistry();
        TimerConfig config = TimerConfig.<List<String>>custom()
                .successResultNameResolver(output -> {
                    then(output).containsExactlyInAnyOrderElementsOf(messages);
                    return String.valueOf(output.size());
                })
                .build();
        Timer timer = Timer.of("timer 1", registry, config);
        List<String> output = Flux.fromIterable(messages)
                .transformDeferred(TimerOperator.of(timer))
                .collectList()
                .block(ofSeconds(1));

        then(output).containsExactlyInAnyOrderElementsOf(messages);
        thenSuccessTimed(registry, timer, output);
    }

    @Test
    public void shouldTimeSuccessfulEmptyFlux() {
        MeterRegistry registry = new SimpleMeterRegistry();
        TimerConfig config = TimerConfig.<List<String>>custom()
                .successResultNameResolver(output -> {
                    then(output).isEmpty();
                    return String.valueOf(output.size());
                })
                .build();
        Timer timer = Timer.of("timer 1", registry, config);
        List<Object> output = Flux.empty()
                .transformDeferred(TimerOperator.of(timer))
                .collectList()
                .block(ofSeconds(1));

        then(output).isEmpty();
        thenSuccessTimed(registry, timer, output);
    }

    @Test
    public void shouldTimeFailedFlux() {
        IllegalStateException exception = new IllegalStateException();
        MeterRegistry registry = new SimpleMeterRegistry();
        TimerConfig config = TimerConfig.custom()
                .failureResultNameResolver(ex -> {
                    then(ex).isEqualTo(exception);
                    return ex.toString();
                })
                .build();
        Timer timer = Timer.of("timer 1", registry, config);
        try {
            Flux.error(exception)
                    .transformDeferred(TimerOperator.of(timer))
                    .collectList()
                    .block(ofSeconds(1));

            failBecauseExceptionWasNotThrown(exception.getClass());
        } catch (Exception e) {
            thenFailureTimed(registry, timer, e);
        }
    }
}
