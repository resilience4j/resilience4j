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

import io.github.resilience4j.AbstractObserver;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.Timer.Context;
import io.reactivex.Observable;
import io.reactivex.Observer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;

class ObservableTimer<T> extends Observable<T> {

    private final Observable<T> upstream;
    private final Timer timer;

    ObservableTimer(Observable<T> upstream, Timer timer) {
        this.upstream = upstream;
        this.timer = timer;
    }

    @Override
    protected void subscribeActual(Observer<? super T> downstream) {
        upstream.subscribe(new TimerObserver(downstream, timer));
    }

    class TimerObserver extends AbstractObserver<T> {

        private final Context context;
        private final KeySetView<ValueWrapper<T>, Boolean> result = newKeySet();

        TimerObserver(Observer<? super T> downstreamObserver, Timer timer) {
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
