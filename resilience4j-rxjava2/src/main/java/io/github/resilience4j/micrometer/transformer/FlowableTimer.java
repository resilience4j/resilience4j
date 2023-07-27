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
package io.github.resilience4j.micrometer.transformer;

import io.github.resilience4j.AbstractSubscriber;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.Timer.Context;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;

class FlowableTimer<T> extends Flowable<T> {

    private final Timer timer;
    private final Publisher<T> upstream;

    FlowableTimer(Publisher<T> upstream, Timer timer) {
        this.timer = timer;
        this.upstream = upstream;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> downstream) {
        upstream.subscribe(new TimerSubscriber(downstream, timer));
    }

    class TimerSubscriber extends AbstractSubscriber<T> {

        private final Context context;
        private final KeySetView<ValueWrapper<T>, Boolean> result = newKeySet();

        TimerSubscriber(Subscriber<? super T> downstreamObserver, Timer timer) {
            super(downstreamObserver);
            context = timer.createContext();
        }

        @Override
        public void onNext(T item) {
            result.add(new ValueWrapper<>(item));
            super.onNext(item);
        }

        @Override
        protected void hookOnError(Throwable e) {
            context.onFailure(e);
        }

        @Override
        protected void hookOnComplete() {
            List<T> items = result.stream().map(ValueWrapper::getValue).toList();
            context.onResult(items);
        }

        @Override
        protected void hookOnCancel() {
            context.onSuccess();
        }
    }
}
