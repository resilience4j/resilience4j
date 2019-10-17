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
package io.github.resilience4j.retry.monitoring.endpoint;


import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEndpointResponse;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


/**
 * REST API endpoint to retrieve all configured retries
 */
@ConfigurationProperties(prefix = "endpoints.retries")
public class RetryEndpoint extends AbstractEndpoint {

    private final RetryRegistry retryRegistry;

    public RetryEndpoint(RetryRegistry retryRegistry) {
        super("retries");
        this.retryRegistry = retryRegistry;
    }

    @Override
    public RetryEndpointResponse invoke() {
        List<String> retries = retryRegistry.getAllRetries()
            .map(Retry::getName).sorted().toJavaList();
        return new RetryEndpointResponse(retries);
    }
}
