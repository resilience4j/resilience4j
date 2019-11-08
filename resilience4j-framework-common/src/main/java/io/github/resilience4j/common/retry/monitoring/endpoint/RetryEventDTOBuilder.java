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

import io.github.resilience4j.retry.event.RetryEvent;

/**
 * retry event DTO builder class
 */
class RetryEventDTOBuilder {

    private final String retryName;
    private final RetryEvent.Type type;
    private final String creationTime;
    private String errorMessage;
    private int numberOfAttempts;


    RetryEventDTOBuilder(String retryName, RetryEvent.Type type, String creationTime) {
        this.retryName = retryName;
        this.type = type;
        this.creationTime = creationTime;
    }

    RetryEventDTOBuilder throwable(Throwable throwable) {
        this.errorMessage = throwable.toString();
        return this;
    }

    RetryEventDTOBuilder numberOfAttempts(int numberOfAttempts) {
        this.numberOfAttempts = numberOfAttempts;
        return this;
    }


    RetryEventDTO build() {
        return new RetryEventDTO(retryName, type, creationTime, errorMessage, numberOfAttempts);
    }
}