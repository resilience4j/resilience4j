/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.monitoring.endpoint;

import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;

public class BulkheadEventEmitter {

    private static final Logger LOG = LoggerFactory.getLogger(BulkheadEventEmitter.class);

    private final SseEmitter sseEmitter;
    private final Disposable disposable;

    private BulkheadEventEmitter(Flux<BulkheadEventDTO> eventStream) {
        this.sseEmitter = new SseEmitter();
        this.sseEmitter.onCompletion(this::unsubscribe);
        this.sseEmitter.onTimeout(this::unsubscribe);
        this.disposable = eventStream.subscribe(this::notify,
                        this.sseEmitter::completeWithError,
                        this.sseEmitter::complete);
    }

    public static SseEmitter createSseEmitter(Flux<BulkheadEvent> eventStream) {
        return new BulkheadEventEmitter(eventStream.map(BulkheadEventDTOFactory::createBulkheadEventDTOFactory)).sseEmitter;
    }

    private void notify(BulkheadEventDTO bulkheadEvent){
        try {
            sseEmitter.send(bulkheadEvent, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            LOG.warn("Failed to send bulkhead event", e);
        }
    }

    private void unsubscribe() {
        this.disposable.dispose();
    }
}
