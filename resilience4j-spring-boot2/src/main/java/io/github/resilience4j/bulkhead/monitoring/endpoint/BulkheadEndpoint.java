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
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.List;


/**
 * {@link Endpoint} to expose Bulkhead events.
 */
@Endpoint(id = "bulkheads")
public class BulkheadEndpoint {

    private final BulkheadRegistry bulkheadRegistry;

    public BulkheadEndpoint(BulkheadRegistry bulkheadRegistry) {
        this.bulkheadRegistry = bulkheadRegistry;
    }

    @ReadOperation
    public BulkheadEndpointResponse getAllBulkheads() {
        List<String> bulkheads = bulkheadRegistry.getAllBulkheads()
                .map(Bulkhead::getName).sorted().toJavaList();
        return new BulkheadEndpointResponse(bulkheads);
    }
}
