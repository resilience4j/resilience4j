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

package io.github.resilience4j.timelimiter.monitoring.endpoint;

import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEndpointResponse;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.List;
import java.util.stream.Collectors;

@Endpoint(id = "timelimiters")
public class TimeLimiterEndpoint {
    private final TimeLimiterRegistry timeLimiterRegistry;

    public TimeLimiterEndpoint(TimeLimiterRegistry timeLimiterRegistry) {
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    @ReadOperation
    public TimeLimiterEndpointResponse getAllTimeLimiters() {
        List<String> timeLimiters = timeLimiterRegistry.getAllTimeLimiters().stream()
                .map(TimeLimiter::getName).sorted().collect(Collectors.toList());
        return new TimeLimiterEndpointResponse(timeLimiters);
    }

}
