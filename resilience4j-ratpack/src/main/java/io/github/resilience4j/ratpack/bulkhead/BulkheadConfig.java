/*
 * Copyright 2017 Jan Sykora
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
package io.github.resilience4j.ratpack.bulkhead;

/**
 * Bulkhead config adapter for integration with Ratpack. {@link #maxWaitTime} should
 * almost always be set to 0, so the compute threads would not be blocked upon execution.
 */
public class BulkheadConfig {

    private boolean defaults = false;
    private int maxConcurrentCalls;
    private long maxWaitTime;

    public BulkheadConfig defaults(boolean defaults) {
        this.defaults = defaults;
        return this;
    }

    public BulkheadConfig maxConcurrentCalls(int maxConcurrentCalls) {
        this.maxConcurrentCalls = maxConcurrentCalls;
        return this;
    }

    public BulkheadConfig maxWaitTime(int maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
        return this;
    }

    public boolean getDefaults() {
        return defaults;
    }

    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    public long getMaxWaitTime() {
        return maxWaitTime;
    }
}
