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
package io.github.resilience4j.reactor.adapter;


import io.github.resilience4j.core.EventPublisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

public class ReactorAdapter {

    private ReactorAdapter() {
    }

    /**
     * Converts the EventPublisher into a Flux.
     *
     * @param eventPublisher the event publisher
     * @param <T>            the type of the event
     * @return the Flux
     */
    public static <T> Flux<T> toFlux(EventPublisher<T> eventPublisher) {
        DirectProcessor<T> directProcessor = DirectProcessor.create();
        eventPublisher.onEvent(directProcessor::onNext);
        return directProcessor;
    }
}
