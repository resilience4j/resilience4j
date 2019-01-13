/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead.operator;


import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.CompletableObserver;
import io.reactivex.CompletableOperator;
import io.reactivex.FlowableOperator;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeOperator;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOperator;
import org.reactivestreams.Subscriber;

/**
 * A RxJava operator which wraps a reactive type in a bulkhead.
 * Please note that Bulkhead shouldn't use wait time as it may block while acquiring the permit.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class BulkheadOperator<T> implements ObservableOperator<T, T>, FlowableOperator<T, T>, SingleOperator<T, T>, CompletableOperator, MaybeOperator<T, T> {
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
    public Subscriber<? super T> apply(Subscriber<? super T> childSubscriber) throws Exception {
        return new BulkheadSubscriber<>(bulkhead, childSubscriber);
    }

    @Override
    public Observer<? super T> apply(Observer<? super T> childObserver) throws Exception {
        return new BulkheadObserver<>(bulkhead, childObserver);
    }

    @Override
    public SingleObserver<? super T> apply(SingleObserver<? super T> childObserver) throws Exception {
        return new BulkheadSingleObserver<>(bulkhead, childObserver);
    }

    @Override
    public CompletableObserver apply(CompletableObserver childObserver) throws Exception {
        return new BulkheadCompletableObserver(bulkhead, childObserver);
    }

    @Override
    public MaybeObserver<? super T> apply(MaybeObserver<? super T> childObserver) throws Exception {
        return new BulkheadMaybeObserver<>(bulkhead, childObserver);
    }
}
