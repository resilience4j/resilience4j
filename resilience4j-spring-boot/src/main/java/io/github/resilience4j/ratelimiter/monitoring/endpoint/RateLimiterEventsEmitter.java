/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.monitoring.endpoint;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.monitoring.model.RateLimiterEventDTO;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

import java.io.IOException;

public class RateLimiterEventsEmitter {
    private final SseEmitter sseEmitter;
    private final Disposable disposable;

    public RateLimiterEventsEmitter(Flowable<RateLimiterEventDTO> flowable) {
        this.sseEmitter = new SseEmitter();
        this.sseEmitter.onCompletion(this::unsubscribe);
        this.sseEmitter.onTimeout(this::unsubscribe);
        this.disposable = flowable.subscribe(this::notify,
            this.sseEmitter::completeWithError,
            this.sseEmitter::complete);
    }

    private void notify(RateLimiterEventDTO rateLimiterEventDTO) throws IOException {
        sseEmitter.send(rateLimiterEventDTO, MediaType.APPLICATION_JSON);
    }

    private void unsubscribe() {
        this.disposable.dispose();
    }

    public static SseEmitter createSseEmitter(Flowable<RateLimiterEvent> eventStream) {
        Flowable<RateLimiterEventDTO> flowable = eventStream.map(RateLimiterEventDTO::createRateLimiterEventDTO);
        return new RateLimiterEventsEmitter(flowable).sseEmitter;
    }
}
