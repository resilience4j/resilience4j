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


import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.reactivex.Flowable;

import java.util.function.Consumer;

public class EventDispatcher implements RateLimiter.EventConsumer, io.reactivex.functions.Consumer<RateLimiterEvent> {

    private volatile Consumer<RateLimiterOnSuccessEvent> onSuccessEventConsumer;
    private volatile Consumer<RateLimiterOnFailureEvent> onOnFailureEventConsumer;

    EventDispatcher(Flowable<RateLimiterEvent> eventStream) {
        eventStream.subscribe(this);
    }

    @Override
    public void accept(RateLimiterEvent event) throws Exception {
        RateLimiterEvent.Type eventType = event.getEventType();
        switch (eventType) {
            case SUCCESSFUL_ACQUIRE:
                if(onSuccessEventConsumer != null){
                    onSuccessEventConsumer.accept((RateLimiterOnSuccessEvent) event);
                }
                break;
            case FAILED_ACQUIRE:
                if(onOnFailureEventConsumer != null) {
                    onOnFailureEventConsumer.accept((RateLimiterOnFailureEvent) event);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public RateLimiter.EventConsumer onSuccess(Consumer<RateLimiterOnSuccessEvent> onSuccessEventConsumer) {
        this.onSuccessEventConsumer = onSuccessEventConsumer;
        return this;
    }

    @Override
    public RateLimiter.EventConsumer onFailure(Consumer<RateLimiterOnFailureEvent> onOnFailureEventConsumer) {
        this.onOnFailureEventConsumer = onOnFailureEventConsumer;
        return this;
    }
}