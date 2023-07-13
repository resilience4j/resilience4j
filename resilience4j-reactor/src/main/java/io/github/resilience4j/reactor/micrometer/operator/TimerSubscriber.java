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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

/**
 * A Reactor {@link Subscriber} to wrap another subscriber in a timer.
 *
 * @param <T> the value type of the upstream and downstream
 */
class TimerSubscriber<T> extends AbstractSubscriber<T> {

    private final Context context;
    private final AtomicBoolean successSignaled = new AtomicBoolean(false);
    private final boolean singleProducer;
    private final AtomicReference<T> singleProducerOutput = new AtomicReference<>();
    private final KeySetView<ValueWrapper<T>, Boolean> multipleProducerOutput = newKeySet();

    protected TimerSubscriber(Timer timer, CoreSubscriber<? super T> downstreamSubscriber, boolean singleProducer) {
        super(downstreamSubscriber);
        this.singleProducer = singleProducer;
        context = requireNonNull(timer, "Timer is null").createContext();
    }

    @Override
    protected void hookOnNext(T value) {
        if (!isDisposed()) {
            if (singleProducer) {
                singleProducerOutput.set(value);
            } else {
                multipleProducerOutput.add(new ValueWrapper<>(value));
            }
            downstreamSubscriber.onNext(value);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (successSignaled.compareAndSet(false, true)) {
            if (singleProducer) {
                context.onSuccess(singleProducerOutput.get());
            } else {
                List<T> output = multipleProducerOutput.stream().map(ValueWrapper::getValue).toList();
                context.onSuccess(output);
            }
        }
        downstreamSubscriber.onComplete();
    }

    @Override
    public void hookOnCancel() {
        if (!successSignaled.get()) {
            context.onSuccess(null);
        }
    }

    @Override
    protected void hookOnError(Throwable e) {
        context.onFailure(e);
        downstreamSubscriber.onError(e);
    }

    /**
     * Wraps a value to prevent the same values be treated as equal ones when adding to Set.
     *
     * @param <T> value type
     */
    private static class ValueWrapper<T> {

        private final T value;

        private ValueWrapper(T value) {
            this.value = value;
        }

        private T getValue() {
            return value;
        }
    }
}
