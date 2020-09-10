/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.consumer;

import io.github.resilience4j.core.lang.Nullable;

import java.util.List;


public interface EventConsumerRegistry<T> {

    /**
     * Creates a new EventConsumer and stores the instance in the registry.
     *
     * @param id         the id of the EventConsumer
     * @param bufferSize the size of the EventConsumer
     * @return a new EventConsumer
     */
    CircularEventConsumer<T> createEventConsumer(String id, int bufferSize);

    /**
     * Returns the EventConsumer to which the specified id is mapped.
     *
     * @param id the id of the EventConsumer
     * @return the EventConsumer to which the specified id is mapped
     */
    @Nullable
    CircularEventConsumer<T> getEventConsumer(String id);

    /**
     * Returns all EventConsumer instances.
     *
     * @return all EventConsumer instances
     */
    List<CircularEventConsumer<T>> getAllEventConsumer();
}
