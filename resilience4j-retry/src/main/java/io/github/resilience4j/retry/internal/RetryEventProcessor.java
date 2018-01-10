/*
 *
 *  Copyright 2018 Jan Sykora at GoodData(R) Corporation
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

package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;

class RetryEventProcessor extends EventProcessor<RetryEvent> implements EventConsumer<RetryEvent>, Retry.EventPublisher {

    @Override
    public void consumeEvent(RetryEvent event) {
        super.processEvent(event);
    }

    @Override
    public Retry.EventPublisher onSuccess(EventConsumer<RetryOnSuccessEvent> onSuccessEventConsumer) {
        registerConsumer(RetryOnSuccessEvent.class, onSuccessEventConsumer);
        return this;
    }

    @Override
    public Retry.EventPublisher onError(EventConsumer<RetryOnErrorEvent> onErrorEventConsumer) {
        registerConsumer(RetryOnErrorEvent.class, onErrorEventConsumer);
        return this;
    }

    @Override
    public Retry.EventPublisher onIgnoredError(EventConsumer<RetryOnIgnoredErrorEvent> onIgnoredErrorEventConsumer) {
        registerConsumer(RetryOnIgnoredErrorEvent.class, onIgnoredErrorEventConsumer);
        return this;
    }
}
