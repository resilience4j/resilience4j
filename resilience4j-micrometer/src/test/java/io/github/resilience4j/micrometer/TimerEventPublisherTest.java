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

import io.github.resilience4j.micrometer.event.TimerOnFailureEvent;
import io.github.resilience4j.micrometer.event.TimerOnStartEvent;
import io.github.resilience4j.micrometer.event.TimerOnSuccessEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.resilience4j.micrometer.event.TimerEvent.Type.*;
import static java.time.ZonedDateTime.now;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.rules.ExpectedException.none;

public class TimerEventPublisherTest {

    @Rule
    public ExpectedException expectedException = none();

    @Test
    public void shouldHandleOnStartEvent() {
        AtomicReference<TimerOnStartEvent> consumedEvent = new AtomicReference<>();
        Timer timer = Timer.of("some operation", new SimpleMeterRegistry());
        timer.getEventPublisher().onStart(consumedEvent::set);
        timer.executeSupplier(() -> "result");

        then(consumedEvent.get().getTimerName()).isEqualTo("some operation");
        then(consumedEvent.get().getEventType()).isEqualTo(START);
    }

    @Test
    public void shouldHandleOnSuccessEvent() {
        AtomicReference<TimerOnSuccessEvent> consumedEvent = new AtomicReference<>();
        Timer timer = Timer.of("some operation", new SimpleMeterRegistry());
        timer.getEventPublisher().onSuccess(consumedEvent::set);

        timer.executeRunnable(() -> {
        });
        then(consumedEvent.get().getTimerName()).isEqualTo("some operation");
        then(consumedEvent.get().getEventType()).isEqualTo(SUCCESS);
        then(consumedEvent.get().getOperationDuration()).isPositive();

        timer.executeSupplier(() -> "result");
        then(consumedEvent.get().getTimerName()).isEqualTo("some operation");
        then(consumedEvent.get().getEventType()).isEqualTo(SUCCESS);
        then(consumedEvent.get().getOperationDuration()).isPositive();
    }

    @Test
    public void shouldHandleOnFailureEvent() {
        expectedException.expect(RuntimeException.class);

        AtomicReference<TimerOnFailureEvent> consumedEvent = new AtomicReference<>();
        Timer timer = Timer.of("some operation", new SimpleMeterRegistry());
        timer.getEventPublisher().onFailure(consumedEvent::set);
        timer.executeSupplier(() -> {
            throw new RuntimeException();
        });

        then(consumedEvent.get().getTimerName()).isEqualTo("some operation");
        then(consumedEvent.get().getEventType()).isEqualTo(FAILURE);
        then(consumedEvent.get().getOperationDuration()).isPositive();
    }
}
