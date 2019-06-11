/*
 * Copyright 2017 Dan Maas
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

package io.github.resilience4j.ratpack;

import io.github.resilience4j.core.lang.Nullable;
import ratpack.func.Function;

import static ratpack.util.Exceptions.uncheck;

public class EndpointsConfig {

    private EndpointConfig circuitBreakers = new EndpointConfig("circuitbreaker");
    private EndpointConfig rateLimiters = new EndpointConfig("ratelimiter");
    private EndpointConfig retries = new EndpointConfig("retry");
    private EndpointConfig bulkheads = new EndpointConfig("bulkhead");
    private EndpointConfig threadPoolBulkheads = new EndpointConfig("threadPoolBulkhead");

    public EndpointConfig getCircuitBreakers() {
        return circuitBreakers;
    }

    public EndpointsConfig circuitBreakers(Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            circuitBreakers = configure.apply(new EndpointConfig("circuitbreaker"));
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public EndpointConfig getRateLimiters() {
        return rateLimiters;
    }

    public EndpointsConfig rateLimiters(Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            rateLimiters = configure.apply(new EndpointConfig("ratelimiter"));
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public EndpointConfig getRetries() {
        return retries;
    }

    public EndpointsConfig retries(Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            retries = configure.apply(new EndpointConfig("retry"));
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public EndpointConfig getBulkheads() {
        return bulkheads;
    }

    public EndpointsConfig bulkheads(Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            bulkheads = configure.apply(new EndpointConfig("bulkhead"));
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public EndpointConfig getThreadPoolBulkheads() {
        return threadPoolBulkheads;
    }

    public EndpointsConfig threadPoolBulkheads(Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            threadPoolBulkheads = configure.apply(new EndpointConfig("threadPoolBulkhead"));
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public static class EndpointConfig {
        private boolean enabled = true;
        @Nullable
        private String path;

        public EndpointConfig() {
        }

        public EndpointConfig(String path) {
            this.path = path;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public EndpointConfig enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @Nullable
        public String getPath() {
            return path;
        }

        public EndpointConfig path(String path) {
            this.path = path;
            return this;
        }

    }
}
