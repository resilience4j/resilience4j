/*
 * Copyright 2019 Julien Hoarau, Robert Winkler
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
package io.github.resilience4j.reactor;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.util.context.Context;

/**
 * Heavily inspired by {@link reactor.core.publisher.BaseSubscriber}
 *
 * @param <T>
 */
public abstract class AbstractSubscriber<T> extends BaseSubscriber<T> {

    protected final CoreSubscriber<? super T> downstreamSubscriber;

    protected AbstractSubscriber(CoreSubscriber<? super T> downstreamSubscriber) {
        this.downstreamSubscriber = downstreamSubscriber;
    }

    /**
     * Hook for further processing of onSubscribe's Subscription.
     *
     * @param subscription the subscription to optionally process
     */
    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        downstreamSubscriber.onSubscribe(this);
    }

    @Override
    public Context currentContext() {
        return downstreamSubscriber.currentContext();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
