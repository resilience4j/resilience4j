/*
 * Copyright 2018 Julien Hoarau
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
import io.github.resilience4j.reactor.FluxResilience;
import io.github.resilience4j.reactor.MonoResilience;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.UnaryOperator;

/**
 * A Reactor operator which wraps a reactive type in a bulkhead.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class BulkheadOperator<T> implements UnaryOperator<Publisher<T>> {
    private final Bulkhead bulkhead;
    private final Scheduler scheduler;

    private BulkheadOperator(Bulkhead bulkhead, Scheduler scheduler) {
        this.bulkhead = bulkhead;
        this.scheduler = scheduler;
    }

    /**
     * Creates a BulkheadOperator.
     *
     * @param <T>      the value type of the upstream and downstream
     * @param bulkhead the Bulkhead
     * @return a BulkheadOperator
     */
    public static <T> BulkheadOperator<T> of(Bulkhead bulkhead) {
        return of(bulkhead, Schedulers.parallel());
    }

    /**
     * Creates a BulkheadOperator.
     *
     * @param <T>       the value type of the upstream and downstream
     * @param bulkhead  the Bulkhead
     * @param scheduler the {@link Scheduler} where to publish
     * @return a BulkheadOperator
     */
    public static <T> BulkheadOperator<T> of(Bulkhead bulkhead, Scheduler scheduler) {
        return new BulkheadOperator<>(bulkhead, scheduler);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return MonoResilience
                    .onAssembly(new MonoBulkhead<>((Mono<? extends T>) publisher, bulkhead, scheduler));
        } else if (publisher instanceof Flux) {
            return FluxResilience
                    .onAssembly(new FluxBulkhead<>((Flux<? extends T>) publisher, bulkhead, scheduler));
        }

        throw new IllegalStateException("Publisher of type <" + publisher.getClass().getSimpleName()
                + "> are not supported by this operator");
    }
}
