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

    private EndpointConfig circuitbreaker = new EndpointConfig("circuitbreaker");
    private EndpointConfig ratelimiter = new EndpointConfig("ratelimiter");
    private EndpointConfig retry = new EndpointConfig("retry");
    private EndpointConfig bulkhead = new EndpointConfig("bulkhead");
    private EndpointConfig threadpoolbulkhead = new EndpointConfig("threadpoolbulkhead");

    public EndpointConfig getCircuitbreaker() {
        return circuitbreaker;
    }

    public EndpointsConfig circuitBreakers(
        Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            circuitbreaker = configure.apply(new EndpointConfig("circuitbreaker"));
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public EndpointConfig getRatelimiter() {
        return ratelimiter;
    }

    public EndpointsConfig rateLimiters(
        Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            ratelimiter = configure.apply(new EndpointConfig("ratelimiter"));
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public EndpointConfig getRetry() {
        return retry;
    }

    public EndpointsConfig retries(
        Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            retry = configure.apply(new EndpointConfig("retry"));
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public EndpointConfig getBulkhead() {
        return bulkhead;
    }

    public EndpointsConfig bulkheads(
        Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            bulkhead = configure.apply(new EndpointConfig("bulkhead"));
            return this;
        } catch (Exception e) {
            throw uncheck(e);
        }
    }

    public EndpointConfig getThreadpoolbulkhead() {
        return threadpoolbulkhead;
    }

    public EndpointsConfig threadPoolBulkheads(
        Function<? super EndpointConfig, ? extends EndpointConfig> configure) {
        try {
            threadpoolbulkhead = configure.apply(new EndpointConfig("threadpoolpulkhead"));
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
