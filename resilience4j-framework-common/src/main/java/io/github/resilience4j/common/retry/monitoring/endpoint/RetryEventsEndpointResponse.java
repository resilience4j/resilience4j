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

import io.github.resilience4j.core.lang.Nullable;

import java.util.List;

/**
 * retry events DTP for rest API
 */
public class RetryEventsEndpointResponse {

    @Nullable
    private List<RetryEventDTO> retryEvents;

    public RetryEventsEndpointResponse() {
    }

    public RetryEventsEndpointResponse(List<RetryEventDTO> retryEvents) {
        this.retryEvents = retryEvents;
    }

    @Nullable
    public List<RetryEventDTO> getRetryEvents() {
        return retryEvents;
    }

    public void setRetryEvents(List<RetryEventDTO> retryEvents) {
        this.retryEvents = retryEvents;
    }
}
