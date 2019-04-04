/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.ratpack.retry.endpoint;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.retry.event.RetryEvent;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetryEventDTO {

    @Nullable
    private String retryName;
    @Nullable
    private RetryEvent.Type retryEventType;
    private int numberOfRetryAttempts;
    @Nullable
    private String retryCreationTime;

    public static RetryEventDTO createRetryEventDTO(RetryEvent retryEvent) {
        RetryEventDTO dto = new RetryEventDTO();
        dto.setRetryName(retryEvent.getName());
        dto.setRetryEventType(retryEvent.getEventType());
        dto.setNumberOfRetryAttempts(retryEvent.getNumberOfRetryAttempts());
        dto.setRetryCreationTime(retryEvent.getCreationTime().toString());
        return dto;
    }

    @Nullable
    public String getRetryName() {
        return retryName;
    }

    public void setRetryName(String retryName) {
        this.retryName = retryName;
    }

    @Nullable
    public RetryEvent.Type getRetryEventType() {
        return retryEventType;
    }

    public void setRetryEventType(RetryEvent.Type retryEventType) {
        this.retryEventType = retryEventType;
    }

    public int getNumberOfRetryAttempts() {
        return numberOfRetryAttempts;
    }

    public void setNumberOfRetryAttempts(int numberOfRetryAttempts) {
        this.numberOfRetryAttempts = numberOfRetryAttempts;
    }

    @Nullable
    public String getRetryCreationTime() {
        return retryCreationTime;
    }

    public void setRetryCreationTime(String retryCreationTime) {
        this.retryCreationTime = retryCreationTime;
    }
}
