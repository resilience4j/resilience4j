/*
 * Copyright 2019 Robert Winkler
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
package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.*;
import org.reactivestreams.Publisher;

import static java.util.Objects.requireNonNull;

/**
 * A Bulkhead operator which checks if a subscriber/observer can acquire a permission to subscribe
 * to an upstream Publisher. Otherwise emits a {@link BulkheadFullException}, if the Bulkhead is
 * full.
 *
 * @param <T> the value type
 */
public class BulkheadOperator<T> implements FlowableTransformer<T, T>, SingleTransformer<T, T>,
    MaybeTransformer<T, T>, CompletableTransformer, ObservableTransformer<T, T> {

    private final Bulkhead bulkhead;

    private BulkheadOperator(Bulkhead bulkhead) {
        this.bulkhead = requireNonNull(bulkhead);
    }

    /**
     * Creates a BulkheadOperator.
     *
     * @param bulkhead the Bulkhead
     * @return a BulkheadOperator
     */
    public static <T> BulkheadOperator<T> of(Bulkhead bulkhead) {
        return new BulkheadOperator<>(bulkhead);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableBulkhead<>(upstream, bulkhead);
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return new SingleBulkhead<>(upstream, bulkhead);
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return new CompletableBulkhead(upstream, bulkhead);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return new MaybeBulkhead<>(upstream, bulkhead);
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return new ObserverBulkhead<>(upstream, bulkhead);
    }
}
