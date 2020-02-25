/*
 * Copyright 2019 authors
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

package io.github.resilience4j.rxjava3.timelimiter.transformer;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.reactivex.rxjava3.core.*;
import org.reactivestreams.Publisher;

import java.util.concurrent.TimeUnit;

public class TimeLimiterTransformer<T> implements FlowableTransformer<T, T>,
    ObservableTransformer<T, T>,
    SingleTransformer<T, T>, CompletableTransformer, MaybeTransformer<T, T> {

    private final TimeLimiter timeLimiter;

    private TimeLimiterTransformer(TimeLimiter timeLimiter) {
        this.timeLimiter = timeLimiter;
    }

    /**
     * Creates a TimeLimiterTransformer.
     *
     * @param timeLimiter the TimeLimiter
     * @param <T>         the value type of the upstream and downstream
     * @return a TimeLimiterTransformer
     */
    public static <T> TimeLimiterTransformer<T> of(TimeLimiter timeLimiter) {
        return new TimeLimiterTransformer<>(timeLimiter);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return upstream
            .timeout(getTimeoutInMillis(), TimeUnit.MILLISECONDS)
            .doOnNext(t -> timeLimiter.onSuccess())
            .doOnComplete(timeLimiter::onSuccess)
            .doOnError(timeLimiter::onError);
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return upstream
            .timeout(getTimeoutInMillis(), TimeUnit.MILLISECONDS)
            .doOnNext(t -> timeLimiter.onSuccess())
            .doOnComplete(timeLimiter::onSuccess)
            .doOnError(timeLimiter::onError);
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return upstream
            .timeout(getTimeoutInMillis(), TimeUnit.MILLISECONDS)
            .doOnSuccess(t -> timeLimiter.onSuccess())
            .doOnError(timeLimiter::onError);
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return upstream
            .timeout(getTimeoutInMillis(), TimeUnit.MILLISECONDS)
            .doOnComplete(timeLimiter::onSuccess)
            .doOnError(timeLimiter::onError);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return upstream
            .timeout(getTimeoutInMillis(), TimeUnit.MILLISECONDS)
            .doOnSuccess(t -> timeLimiter.onSuccess())
            .doOnComplete(timeLimiter::onSuccess)
            .doOnError(timeLimiter::onError);
    }

    private long getTimeoutInMillis() {
        return timeLimiter.getTimeLimiterConfig()
            .getTimeoutDuration()
            .toMillis();
    }

}
