/*
 * Copyright 2018 Julien Hoarau, Robert Winkler
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
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.publisher.Operators;

import static io.github.resilience4j.bulkhead.BulkheadFullException.getBulkheadFullException;

class MonoBulkhead<T> extends MonoOperator<T, T> {
    private final Bulkhead bulkhead;

    MonoBulkhead(Mono<? extends T> source, Bulkhead bulkhead) {
        super(source);
        this.bulkhead = bulkhead;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        if(bulkhead.tryAcquirePermission()){
            source.subscribe(new BulkheadSubscriber<>(bulkhead, actual, true));
        }else{
            Operators.error(actual, BulkheadFullException.getBulkheadFullException(bulkhead));
        }
    }
}
