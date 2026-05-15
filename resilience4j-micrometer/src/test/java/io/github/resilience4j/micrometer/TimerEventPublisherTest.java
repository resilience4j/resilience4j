/*
 *  Copyright 2026 Mariusz Kopylec
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
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static io.github.resilience4j.micrometer.event.TimerEvent.Type.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

class TimerEventPublisherTest {

    @Test
    void shouldHandleOnStartEvent() {
        AtomicReference<TimerOnStartEvent> consumedEvent = new AtomicReference<>();
        Timer timer = Timer.of("some operation", new SimpleMeterRegistry());
        timer.getEventPublisher().onStart(consumedEvent::set);
        timer.executeSupplier(() -> "result");

        then(consumedEvent.get().getTimerName()).isEqualTo("some operation");
        then(consumedEvent.get().getEventType()).isEqualTo(START);
    }

    @Test
    void shouldHandleOnSuccessEvent() {
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
    void shouldHandleOnFailureEvent() {
        AtomicReference<TimerOnFailureEvent> consumedEvent = new AtomicReference<>();
        Timer timer = Timer.of("some operation", new SimpleMeterRegistry());
        timer.getEventPublisher().onFailure(consumedEvent::set);

        assertThatThrownBy(() -> timer.executeSupplier(() -> {
            throw new RuntimeException();
        })).isInstanceOf(RuntimeException.class);

        then(consumedEvent.get().getTimerName()).isEqualTo("some operation");
        then(consumedEvent.get().getEventType()).isEqualTo(FAILURE);
        then(consumedEvent.get().getOperationDuration()).isPositive();
    }
}
