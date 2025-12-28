/*
 * Copyright 2025 Mahmoud Romeh, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.micrometer;

import io.github.resilience4j.common.micrometer.monitoring.endpoint.TimerEventDTO;
import io.github.resilience4j.common.micrometer.monitoring.endpoint.TimerEventsEndpointResponse;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.springboot.service.test.TestApplication;
import io.github.resilience4j.springboot.service.test.micrometer.TimedService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.ZonedDateTime;
import java.util.List;

import static io.github.resilience4j.micrometer.TimerAssertions.thenFailureTimed;
import static io.github.resilience4j.micrometer.TimerAssertions.thenSuccessTimed;
import static io.github.resilience4j.micrometer.event.TimerEvent.Type.*;
import static io.github.resilience4j.springboot.service.test.micrometer.TimedService.*;
import static java.time.Duration.ofSeconds;
import static java.time.ZonedDateTime.now;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = TestApplication.class)
@AutoConfigureTestRestTemplate
public class TimerTest {

    @Autowired
    private TestRestTemplate httpClient;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private TimerRegistry timerRegistry;
    @Autowired
    private TimedService service;

    @Test
    public void shouldTimeBasicOperation() {
        Timer timer = timerRegistry.timer(BASIC_TIMER_NAME);
        ZonedDateTime now = now();

        String result1 = service.succeedBasic(123);
        thenSuccessTimed(meterRegistry, timer);
        then(result1).isEqualTo("123");

        try {
            service.failBasic();
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        thenEventsPublishedToActuator(timer, now);
    }

    @Test
    public void shouldTimeReactorOperation() {
        Timer timer = timerRegistry.timer(REACTOR_TIMER_NAME);
        ZonedDateTime now = now();

        String result1 = service.succeedReactor(123).block(ofSeconds(1));
        thenSuccessTimed(meterRegistry, timer);
        then(result1).isEqualTo("123");

        try {
            service.failReactor().block(ofSeconds(1));
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        thenEventsPublishedToActuator(timer, now);
    }

    @Test
    public void shouldTimeRxJava2Operation() {
        Timer timer = timerRegistry.timer(RXJAVA2_TIMER_NAME);
        ZonedDateTime now = now();

        String result1 = service.succeedRxJava2(123).blockingGet();
        thenSuccessTimed(meterRegistry, timer);
        then(result1).isEqualTo("123");

        try {
            service.failRxJava2().blockingGet();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            thenFailureTimed(meterRegistry, timer, e);
        }

        thenEventsPublishedToActuator(timer, now);
    }

    private void thenEventsPublishedToActuator(Timer timer, ZonedDateTime now) {
        List<TimerEventDTO> events = httpClient.getForEntity("/actuator/timerevents/" + timer.getName(), TimerEventsEndpointResponse.class).getBody().getTimerEvents();
        then(events).hasSize(4);

        then(events.get(0).getTimerName()).isEqualTo(timer.getName());
        then(events.get(0).getType()).isEqualTo(START);
        then(events.get(0).getOperationDuration()).isNull();

        then(events.get(1).getTimerName()).isEqualTo(timer.getName());
        then(events.get(1).getType()).isEqualTo(SUCCESS);
        then(events.get(1).getOperationDuration()).isPositive();

        then(events.get(2).getTimerName()).isEqualTo(timer.getName());
        then(events.get(2).getType()).isEqualTo(START);
        then(events.get(2).getOperationDuration()).isNull();

        then(events.get(3).getTimerName()).isEqualTo(timer.getName());
        then(events.get(3).getType()).isEqualTo(FAILURE);
        then(events.get(3).getOperationDuration()).isPositive();
    }
}
