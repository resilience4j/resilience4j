/*
 * Copyright 2018 Mariusz Kopylec
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
package io.github.resilience4j.reactor.micrometer.operator;

import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.Timer.Context;
import io.github.resilience4j.reactor.AbstractSubscriber;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber in a timer.
 *
 * @param <T> the value type of the upstream and downstream
 */
class TimerSubscriber<T> extends AbstractSubscriber<T> {

    private final Context context;
    private final AtomicBoolean successSignaled = new AtomicBoolean(false);

    TimerSubscriber(Timer timer, CoreSubscriber<? super T> downstreamSubscriber) {
        super(downstreamSubscriber);
        context = requireNonNull(timer, "Timer is null").createContext();
    }

    @Override
    protected void hookOnNext(T value) {
        if (!isDisposed()) {
            downstreamSubscriber.onNext(value);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (successSignaled.compareAndSet(false, true)) {
            context.onSuccess();
        }
        downstreamSubscriber.onComplete();
    }

    @Override
    protected void hookOnCancel() {
        if (!successSignaled.get()) {
            context.onSuccess();
        }
    }

    @Override
    protected void hookOnError(Throwable e) {
        context.onFailure(e);
        downstreamSubscriber.onError(e);
    }
}
