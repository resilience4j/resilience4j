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

import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.Timer.Context;
import io.reactivex.*;
import org.reactivestreams.Publisher;

public class TimerTransformer<T> implements FlowableTransformer<T, T>, ObservableTransformer<T, T>, SingleTransformer<T, T>, CompletableTransformer, MaybeTransformer<T, T> {

    private final Timer timer;

    private TimerTransformer(Timer timer) {
        this.timer = timer;
    }

    /**
     * Creates a TimerTransformer.
     *
     * @param timer the Timer
     * @param <T>   the value type of the upstream and downstream
     * @return a TimerTransformer
     */
    public static <T> TimerTransformer<T> of(Timer timer) {
        return new TimerTransformer<>(timer);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        Context context = timer.createContext();
        return upstream
                .doOnNext(context::onSuccess)
                .doOnError(context::onFailure);
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        Context context = timer.createContext();
        return upstream
                .doOnNext(context::onSuccess)
                .doOnError(context::onFailure);
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        Context context = timer.createContext();
        return upstream
                .doOnSuccess(context::onSuccess)
                .doOnError(context::onFailure);
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        Context context = timer.createContext();
        return upstream
                .doOnComplete(() -> context.onSuccess(Void.TYPE))
                .doOnError(context::onFailure);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        Context context = timer.createContext();
        return upstream
                .doOnSuccess(context::onSuccess)
                .doOnError(context::onFailure);
    }
}
