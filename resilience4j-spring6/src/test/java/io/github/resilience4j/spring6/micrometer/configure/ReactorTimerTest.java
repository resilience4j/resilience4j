/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.spring6.micrometer.configure;

import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.spring6.TestApplication;
import io.github.resilience4j.spring6.micrometer.configure.utils.ReactorTimedService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static io.github.resilience4j.micrometer.TimerAssertions.thenFailureTimed;
import static io.github.resilience4j.micrometer.TimerAssertions.thenSuccessTimed;
import static io.github.resilience4j.spring6.micrometer.configure.utils.ReactorTimedService.FLUX_TIMER_NAME;
import static io.github.resilience4j.spring6.micrometer.configure.utils.ReactorTimedService.MONO_TIMER_NAME;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.BDDAssertions.then;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class)
public class ReactorTimerTest {

    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private TimerRegistry timerRegistry;
    @Autowired
    private ReactorTimedService service;

    @Test
    public void shouldTimeMono() {
        Timer timer = timerRegistry.timer(MONO_TIMER_NAME);

        String result1 = service.succeedMono(123).block(ofSeconds(1));
        thenSuccessTimed(meterRegistry, timer);
        then(result1).isEqualTo("123");

        try {
            service.failMono().block(ofSeconds(1));
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        String result2 = service.recoverMono(123).block(ofSeconds(1));
        thenFailureTimed(meterRegistry, timer, new IllegalStateException());
        then(result2).isEqualTo("Mono recovered 123");

        Timer spelTimer = timerRegistry.timer(MONO_TIMER_NAME + "SpEl");
        String result3 = service.recoverMono(spelTimer.getName(), 123).block(ofSeconds(1));
        thenFailureTimed(meterRegistry, spelTimer, new IllegalStateException());
        then(result3).isEqualTo("Mono recovered 123");
    }

    @Test
    public void shouldTimeFlux() {
        Timer timer = timerRegistry.timer(FLUX_TIMER_NAME);

        List<String> result1 = service.succeedFlux(123).collectList().block(ofSeconds(1));
        thenSuccessTimed(meterRegistry, timer);
        then(result1).containsExactly("123");

        try {
            service.failFlux().collectList().block(ofSeconds(1));
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        List<String> result2 = service.recoverFlux(123).collectList().block(ofSeconds(1));
        thenFailureTimed(meterRegistry, timer, new IllegalStateException());
        then(result2).containsExactly("Flux recovered 123");

        Timer spelTimer = timerRegistry.timer(FLUX_TIMER_NAME + "SpEl");
        List<String> result3 = service.recoverFlux(spelTimer.getName(), 123).collectList().block(ofSeconds(1));
        thenFailureTimed(meterRegistry, spelTimer, new IllegalStateException());
        then(result3).containsExactly("Flux recovered 123");
    }
}
