/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.internal;


import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;

public class RateLimiterEventProcessor extends
    io.github.resilience4j.core.EventProcessor<RateLimiterEvent> implements
    EventConsumer<RateLimiterEvent>, RateLimiter.EventPublisher {

    @Override
    public void consumeEvent(RateLimiterEvent event) {
        super.processEvent(event);
    }

    @Override
    public RateLimiter.EventPublisher onSuccess(
        EventConsumer<RateLimiterOnSuccessEvent> onSuccessEventConsumer) {
        registerConsumer(RateLimiterOnSuccessEvent.class.getSimpleName(), onSuccessEventConsumer);
        return this;
    }

    @Override
    public RateLimiter.EventPublisher onFailure(
        EventConsumer<RateLimiterOnFailureEvent> onOnFailureEventConsumer) {
        registerConsumer(RateLimiterOnFailureEvent.class.getSimpleName(), onOnFailureEventConsumer);
        return this;
    }
}