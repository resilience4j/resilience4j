/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.common.bulkhead.monitoring.endpoint;

import io.github.resilience4j.bulkhead.event.BulkheadEvent;

public class BulkheadEventDTO {

    private String bulkheadName;
    private BulkheadEvent.Type type;
    private String creationTime;

    BulkheadEventDTO() {
    }

    BulkheadEventDTO(String bulkheadName,
        BulkheadEvent.Type type,
        String creationTime) {
        this.bulkheadName = bulkheadName;
        this.type = type;
        this.creationTime = creationTime;
    }

    public String getBulkheadName() {
        return bulkheadName;
    }

    public void setBulkheadName(String bulkheadName) {
        this.bulkheadName = bulkheadName;
    }

    public BulkheadEvent.Type getType() {
        return type;
    }

    public void setType(BulkheadEvent.Type type) {
        this.type = type;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }
}
