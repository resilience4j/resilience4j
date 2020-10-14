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
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;

public class TimeLimiterEventDTO {

    @Nullable
    private String timeLimiterName;
    @Nullable private TimeLimiterEvent.Type type;
    @Nullable private String creationTime;

    public static TimeLimiterEventDTO createTimeLimiterEventDTO(TimeLimiterEvent timeLimiterEvent) {
        TimeLimiterEventDTO dto = new TimeLimiterEventDTO();
        dto.setTimeLimiterName(timeLimiterEvent.getTimeLimiterName());
        dto.setType(timeLimiterEvent.getEventType());
        dto.setCreationTime(timeLimiterEvent.getCreationTime().toString());
        return dto;
    }

    @Nullable
    public String getTimeLimiterName() {
        return timeLimiterName;
    }

    public void setTimeLimiterName(@Nullable String timeLimiterName) {
        this.timeLimiterName = timeLimiterName;
    }

    @Nullable
    public TimeLimiterEvent.Type getType() {
        return type;
    }

    public void setType(@Nullable TimeLimiterEvent.Type type) {
        this.type = type;
    }

    @Nullable
    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(@Nullable String creationTime) {
        this.creationTime = creationTime;
    }
}
