/*
 * Copyright 2020 Ingyu Hwang
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

package io.github.resilience4j.common.timelimiter.monitoring.endpoint;

import io.github.resilience4j.core.lang.Nullable;

import java.util.List;

public class TimeLimiterEventsEndpointResponse {

    @Nullable
    private List<TimeLimiterEventDTO> timeLimiterEvents;

    public TimeLimiterEventsEndpointResponse() {
    }

    public TimeLimiterEventsEndpointResponse(@Nullable List<TimeLimiterEventDTO> timeLimiterEvents) {
        this.timeLimiterEvents = timeLimiterEvents;
    }

    @Nullable
    public List<TimeLimiterEventDTO> getTimeLimiterEvents() {
        return timeLimiterEvents;
    }

    public void setTimeLimiterEvents(@Nullable List<TimeLimiterEventDTO> timeLimiterEvents) {
        this.timeLimiterEvents = timeLimiterEvents;
    }

}
