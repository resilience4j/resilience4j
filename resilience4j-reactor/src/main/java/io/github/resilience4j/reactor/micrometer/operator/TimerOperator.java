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
import io.github.resilience4j.reactor.IllegalPublisherException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.UnaryOperator;

/**
 * A Reactor Time operator which wraps a reactive type in a Timer.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class TimerOperator<T> implements UnaryOperator<Publisher<T>> {

    private final Timer timer;

    private TimerOperator(Timer timer) {
        this.timer = timer;
    }

    /**
     * Creates a TimerOperator.
     *
     * @param <T>   the value type of the upstream and downstream
     * @param timer the timer
     * @return a TimerOperator
     */
    public static <T> TimerOperator<T> of(Timer timer) {
        return new TimerOperator<>(timer);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return new MonoTimer<>((Mono<? extends T>) publisher, timer);
        } else if (publisher instanceof Flux) {
            return new FluxTimer<>((Flux<? extends T>) publisher, timer);
        } else {
            throw new IllegalPublisherException(publisher);
        }
    }
}
