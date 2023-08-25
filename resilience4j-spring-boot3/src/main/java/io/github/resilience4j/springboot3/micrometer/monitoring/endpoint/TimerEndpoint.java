/*
 * Copyright 2023 Mariusz Kopylec
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
package io.github.resilience4j.springboot3.micrometer.monitoring.endpoint;


import io.github.resilience4j.common.micrometer.monitoring.endpoint.TimerEndpointResponse;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerRegistry;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * REST API endpoint to retrieve all configured timers
 */
@Endpoint(id = "timers")
public class TimerEndpoint {

    private final TimerRegistry timerRegistry;

    public TimerEndpoint(TimerRegistry timerRegistry) {
        this.timerRegistry = timerRegistry;
    }

    @ReadOperation
    public TimerEndpointResponse getAllRetries() {
        List<String> retries = timerRegistry.getAllTimers()
                .map(Timer::getName)
                .sorted()
                .collect(toList());
        return new TimerEndpointResponse(retries);
    }
}
