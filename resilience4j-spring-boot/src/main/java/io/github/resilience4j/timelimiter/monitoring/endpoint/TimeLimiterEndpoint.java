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
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * {@link Endpoint} to expose TimeLimiter.
 */
@ConfigurationProperties(prefix = "endpoints.timelimiter")
public class TimeLimiterEndpoint extends AbstractEndpoint {

    private final TimeLimiterRegistry timeLimiterRegistry;

    public TimeLimiterEndpoint(TimeLimiterRegistry timeLimiterRegistry) {
        super("timelimiter");
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    @Override
    public Object invoke() {
        List<String> names = timeLimiterRegistry.getAllTimeLimiters()
                .map(TimeLimiter::getName).sorted().toJavaList();
        return ResponseEntity.ok(new TimeLimiterEndpointResponse(names));
    }

}
