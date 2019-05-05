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
package io.github.resilience4j.ratpack.ratelimiter.endpoint;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RateLimiterEventDTO {

    @Nullable
    private String rateLimiterName;
    @Nullable
    private RateLimiterEvent.Type type;
    @Nullable
    private String creationTime;

    public static RateLimiterEventDTO createRateLimiterEventDTO(RateLimiterEvent rateLimiterEvent) {
        RateLimiterEventDTO dto = new RateLimiterEventDTO();
        dto.setRateLimiterName(rateLimiterEvent.getRateLimiterName());
        dto.setType(rateLimiterEvent.getEventType());
        dto.setCreationTime(rateLimiterEvent.getCreationTime().toString());
        return dto;
    }

    @Nullable
    public String getRateLimiterName() {
        return rateLimiterName;
    }

    public void setRateLimiterName(String rateLimiterName) {
        this.rateLimiterName = rateLimiterName;
    }

    @Nullable
    public RateLimiterEvent.Type getType() {
        return type;
    }

    public void setType(RateLimiterEvent.Type type) {
        this.type = type;
    }

    @Nullable
    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }
}
