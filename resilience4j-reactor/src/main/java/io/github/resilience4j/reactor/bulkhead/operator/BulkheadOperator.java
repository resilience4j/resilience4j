/*
 * Copyright 2019 Julien Hoarau, Robert Winkler
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
package io.github.resilience4j.reactor.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.reactor.IllegalPublisherException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.UnaryOperator;

import org.reactivestreams.Publisher;

/**
 * A Bulkhead operator which checks if a subscriber/observer can acquire a permission to subscribe to an upstream Publisher.
 * Otherwise emits a {@link BulkheadFullException}, if the Bulkhead is full.
 *
 * @param <T> the value type
 */
public class BulkheadOperator<T> implements UnaryOperator<Publisher<T>> {
    private final Bulkhead bulkhead;

    private BulkheadOperator(Bulkhead bulkhead) {
        this.bulkhead = bulkhead;
    }

    /**
     * Creates a BulkheadOperator.
     *
     * @param <T>      the value type of the upstream and downstream
     * @param bulkhead the Bulkhead
     * @return a BulkheadOperator
     */
    public static <T> BulkheadOperator<T> of(Bulkhead bulkhead) {
        return new BulkheadOperator<>(bulkhead);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return new MonoBulkhead<>((Mono<? extends T>) publisher, bulkhead);
        } else if (publisher instanceof Flux) {
            return new FluxBulkhead<>((Flux<? extends T>) publisher, bulkhead);
        } else {
            throw new IllegalPublisherException(publisher);
        }
    }
}
