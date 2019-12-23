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
 * Retry event data DTO which will be returned from the REST API
 */
public class RetryEventDTO {

    private String retryName;
    private RetryEvent.Type type;
    private String creationTime;
    private String errorMessage;
    private int numberOfAttempts;

    RetryEventDTO() {
    }

    RetryEventDTO(String retryName,
        RetryEvent.Type type,
        String creationTime,
        String errorMessage,
        int numberOfAttempts) {
        this.retryName = retryName;
        this.type = type;
        this.creationTime = creationTime;
        this.errorMessage = errorMessage;
        this.numberOfAttempts = numberOfAttempts;
    }

    public String getRetryName() {
        return retryName;
    }

    public void setRetryName(String retryName) {
        this.retryName = retryName;
    }

    public RetryEvent.Type getType() {
        return type;
    }

    public void setType(RetryEvent.Type type) {
        this.type = type;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }

    public void setNumberOfAttempts(int numberOfAttempts) {
        this.numberOfAttempts = numberOfAttempts;
    }

}
