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
package io.github.resilience4j.micrometer.internal;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.micrometer.event.TimerEvent;
import io.github.resilience4j.micrometer.event.TimerOnFailureEvent;
import io.github.resilience4j.micrometer.event.TimerOnStartEvent;
import io.github.resilience4j.micrometer.event.TimerOnSuccessEvent;

import static io.github.resilience4j.micrometer.Timer.EventPublisher;

public class TimerEventProcessor extends EventProcessor<TimerEvent> implements EventConsumer<TimerEvent>, EventPublisher {

    @Override
    public void consumeEvent(@NonNull TimerEvent event) {
        super.processEvent(event);
    }

    @Override
    public EventPublisher onStart(EventConsumer<TimerOnStartEvent> eventConsumer) {
        registerConsumer(TimerOnStartEvent.class.getName(), eventConsumer);
        return this;
    }

    @Override
    public EventPublisher onSuccess(EventConsumer<TimerOnSuccessEvent> eventConsumer) {
        registerConsumer(TimerOnSuccessEvent.class.getName(), eventConsumer);
        return this;
    }

    @Override
    public EventPublisher onFailure(EventConsumer<TimerOnFailureEvent> eventConsumer) {
        registerConsumer(TimerOnFailureEvent.class.getName(), eventConsumer);
        return this;
    }
}
