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
import io.reactivex.*;
import org.reactivestreams.Publisher;

import static java.util.Objects.requireNonNull;

public class TimerTransformer<T> implements FlowableTransformer<T, T>, SingleTransformer<T, T>, MaybeTransformer<T, T>, CompletableTransformer, ObservableTransformer<T, T> {

    private final Timer timer;

    private TimerTransformer(Timer timer) {
        this.timer = requireNonNull(timer, "timer is null");
    }

    public static <T> TimerTransformer<T> of(Timer timer) {
        return new TimerTransformer<>(timer);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableTimer<>(upstream, timer);
    }


    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return new SingleTimer<>(upstream, timer);
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return new CompletableTimer(upstream, timer);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return new MaybeTimer<>(upstream, timer);
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return new ObservableTimer<>(upstream, timer);
    }
}
