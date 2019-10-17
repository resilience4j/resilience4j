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
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * {@link Endpoint} to expose RateLimiter events.
 */
@ConfigurationProperties(prefix = "endpoints.ratelimiter")
public class RateLimiterEndpoint extends AbstractEndpoint {

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimiterEndpoint(RateLimiterRegistry rateLimiterRegistry) {
        super("ratelimiter");
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Object invoke() {
        List<String> names = rateLimiterRegistry.getAllRateLimiters()
            .map(RateLimiter::getName).sorted().toJavaList();
        return ResponseEntity.ok(new RateLimiterEndpointResponse(names));
    }
}
