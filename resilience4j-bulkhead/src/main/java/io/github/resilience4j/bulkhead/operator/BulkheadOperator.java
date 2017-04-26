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
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A RxJava operator which wraps an Observable or Flowable in a bulkhead
 *
 * @param <T> the value type of the upstream and downstream
 */
public class BulkheadOperator<T> implements ObservableOperator<T, T>, FlowableOperator<T, T>, SingleOperator<T, T> {

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
        return new BulkheadSubscriber(childSubscriber);
    }

    @Override
    public Observer<? super T> apply(Observer<? super T> childObserver) throws Exception {
        return new BulkheadObserver(childObserver);
    }

    @Override
    public SingleObserver<? super T> apply(SingleObserver<? super T> childObserver) throws Exception {
        return new BulkheadSingleObserver(childObserver);
    }

    private final class BulkheadSubscriber implements Subscriber<T>, Subscription {

        private final Subscriber<? super T> childSubscriber;
        private Subscription subscription;
        private AtomicBoolean cancelled = new AtomicBoolean(false);

        BulkheadSubscriber(Subscriber<? super T> childSubscriber) {
            this.childSubscriber = childSubscriber;
        }

        @Override
        public void onSubscribe(Subscription subscription) {

            this.subscription = subscription;

            if (bulkhead.isCallPermitted()) {
                childSubscriber.onSubscribe(this);
            }
            else {
                subscription.cancel();
                childSubscriber.onSubscribe(this);
                childSubscriber.onError(
                    new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()))
                );
            }
        }

        @Override
        public void onNext(T event) {
            if (!isCancelled()) {
                childSubscriber.onNext(event);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!isCancelled()) {
                bulkhead.onComplete();
                childSubscriber.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!isCancelled()) {
                bulkhead.onComplete();
                childSubscriber.onComplete();
            }
        }

        @Override
        public void request(long n) {
            subscription.request(n);
        }

        @Override
        public void cancel() {
            if (!cancelled.get()) {
                cancelled.set(true);
                bulkhead.onComplete();
                subscription.cancel();
            }
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    private final class BulkheadObserver implements Observer<T>, Disposable {

        private final Observer<? super T> childObserver;
        private Disposable disposable;
        private AtomicBoolean cancelled = new AtomicBoolean(false);

        BulkheadObserver(Observer<? super T> childObserver) {
            this.childObserver = childObserver;
        }

        @Override
        public void onSubscribe(Disposable disposable) {

            this.disposable = disposable;

            if (bulkhead.isCallPermitted()) {
                childObserver.onSubscribe(this);
            }
            else {
                disposable.dispose();
                childObserver.onSubscribe(this);
                childObserver.onError(
                    new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()))
                );
            }
        }

        @Override
        public void onNext(T event) {
            if (!isDisposed()) {
                childObserver.onNext(event);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!isDisposed()) {
                if (!(e instanceof BulkheadFullException)) {
                    bulkhead.onComplete();
                }

                childObserver.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                bulkhead.onComplete();
                childObserver.onComplete();
            }
        }

        @Override
        public void dispose() {
            if (!cancelled.get()) {
                cancelled.set(true);
                bulkhead.onComplete();
                disposable.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled.get();
        }
    }

    private class BulkheadSingleObserver implements SingleObserver<T>, Disposable {

        private final SingleObserver<? super T> childObserver;
        private Disposable disposable;
        private AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile boolean rejected = false;

        BulkheadSingleObserver(SingleObserver<? super T> childObserver) {
            this.childObserver = childObserver;
        }

        @Override
        public void onSubscribe(Disposable disposable) {

            this.disposable = disposable;

            if (bulkhead.isCallPermitted()) {
                childObserver.onSubscribe(this);
            }
            else {
                rejected = true;
                disposable.dispose();
                childObserver.onSubscribe(this);
                childObserver.onError(
                    new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()))
                );
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!isDisposed()) {
                bulkhead.onComplete();
                childObserver.onError(e);
            }
        }

        @Override
        public void onSuccess(T value) {
            if (!isDisposed()) {
                if (!rejected) {
                    bulkhead.onComplete();
                }
                childObserver.onSuccess(value);
            }
        }

        @Override
        public void dispose() {
            if (!cancelled.get()) {
                cancelled.set(true);
                bulkhead.onComplete();
                disposable.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled.get();
        }
    }
}
