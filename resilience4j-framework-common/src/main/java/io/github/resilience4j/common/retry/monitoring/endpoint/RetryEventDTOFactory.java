/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.common.retry.monitoring.endpoint;

import io.github.resilience4j.retry.event.*;

/**
 * retry event DTO factory
 */
public class RetryEventDTOFactory {

    private RetryEventDTOFactory() {
    }

    public static RetryEventDTO createRetryEventDTO(RetryEvent event) {
        switch (event.getEventType()) {
            case ERROR:
                RetryOnErrorEvent onErrorEvent = (RetryOnErrorEvent) event;
                return newRetryEventDTOBuilder(onErrorEvent)
                    .throwable(onErrorEvent.getLastThrowable())
                    .numberOfAttempts(onErrorEvent.getNumberOfRetryAttempts())
                    .build();
            case SUCCESS:
                RetryOnSuccessEvent onSuccessEvent = (RetryOnSuccessEvent) event;
                return newRetryEventDTOBuilder(onSuccessEvent)
                    .numberOfAttempts(onSuccessEvent.getNumberOfRetryAttempts())
                    .throwable(onSuccessEvent.getLastThrowable())
                    .build();
            case RETRY:
                RetryOnRetryEvent onStateTransitionEvent = (RetryOnRetryEvent) event;
                return newRetryEventDTOBuilder(onStateTransitionEvent)
                    .throwable(onStateTransitionEvent.getLastThrowable())
                    .numberOfAttempts(onStateTransitionEvent.getNumberOfRetryAttempts())
                    .build();
            case IGNORED_ERROR:
                RetryOnIgnoredErrorEvent onIgnoredErrorEvent = (RetryOnIgnoredErrorEvent) event;
                return newRetryEventDTOBuilder(onIgnoredErrorEvent)
                    .throwable(onIgnoredErrorEvent.getLastThrowable())
                    .build();
            default:
                throw new IllegalArgumentException("Invalid event");
        }
    }

    private static RetryEventDTOBuilder newRetryEventDTOBuilder(RetryEvent event) {
        return new RetryEventDTOBuilder(event.getName(), event.getEventType(),
            event.getCreationTime().toString());
    }
}
