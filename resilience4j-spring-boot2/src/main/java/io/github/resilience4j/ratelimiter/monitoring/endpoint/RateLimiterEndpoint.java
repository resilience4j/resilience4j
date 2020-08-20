/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.monitoring.endpoint;

import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEndpointResponse;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.List;
import java.util.stream.Collectors;

@Endpoint(id = "ratelimiters")
public class RateLimiterEndpoint {

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimiterEndpoint(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @ReadOperation
    public RateLimiterEndpointResponse getAllRateLimiters() {
        List<String> names = rateLimiterRegistry.getAllRateLimiters().stream()
            .map(RateLimiter::getName).sorted().collect(Collectors.toList());
        return new RateLimiterEndpointResponse(names);
    }
}
