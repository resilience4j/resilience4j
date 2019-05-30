/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.monitoring.endpoint;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;

import java.util.List;


/**
 * {@link Endpoint} to expose Bulkhead events.
 */
@ConfigurationProperties(prefix = "endpoints.bulkhead")
public class BulkheadEndpoint extends AbstractEndpoint {

    private final BulkheadRegistry bulkheadRegistry;

    public BulkheadEndpoint(BulkheadRegistry bulkheadRegistry) {
        super("bulkhead");
        this.bulkheadRegistry = bulkheadRegistry;
    }

    @Override
    public ResponseEntity<BulkheadEndpointResponse> invoke() {
        List<String> bulkheads = bulkheadRegistry.getAllBulkheads()
                .map(Bulkhead::getName).sorted().toJavaList();
        return ResponseEntity.ok(new BulkheadEndpointResponse(bulkheads));
    }
}
