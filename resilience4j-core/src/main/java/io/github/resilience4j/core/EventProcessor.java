/*
 *
 *  Copyright 2017: Robert Winkler
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
package io.github.resilience4j.core;

import io.github.resilience4j.core.lang.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EventProcessor<T> implements EventPublisher<T> {

    private boolean consumerRegistered;
    @Nullable private EventConsumer<T> onEventConsumer;
    private ConcurrentMap<Class<? extends T>, EventConsumer<Object>> eventConsumers = new ConcurrentHashMap<>();

    public boolean hasConsumers(){
        return consumerRegistered;
    }

    @SuppressWarnings("unchecked")
    public synchronized <E extends T> void registerConsumer(Class<? extends E> eventType, EventConsumer<E> eventConsumer){
        consumerRegistered = true;
        eventConsumers.put(eventType, (EventConsumer<Object>) eventConsumer);
    }

    @SuppressWarnings("unchecked")
    public <E extends T> boolean processEvent(E event) {
        boolean consumed = false;
        EventConsumer<T> onEventConsumer = this.onEventConsumer;
        if(onEventConsumer != null){
            onEventConsumer.consumeEvent(event);
            consumed = true;
        }
        if(!eventConsumers.isEmpty()){
            EventConsumer<T> eventConsumer = (EventConsumer<T>) eventConsumers.get(event.getClass());
            if(eventConsumer != null){
                eventConsumer.consumeEvent(event);
                consumed = true;
            }
        }
        return consumed;
    }

    @Override
    public synchronized void onEvent(@Nullable EventConsumer<T> onEventConsumer) {
        consumerRegistered = true;
        this.onEventConsumer = onEventConsumer;
    }
}
