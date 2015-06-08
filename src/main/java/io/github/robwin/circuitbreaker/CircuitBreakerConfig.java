/*
 *
 *  Copyright 2015 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.robwin.circuitbreaker;

import java.util.ArrayList;
import java.util.List;

public class CircuitBreakerConfig {
    // The maximum number of allowed failures
    private int maxFailures;
    // The wait interval which specifies how long the CircuitBreaker should stay OPEN
    private int waitInterval;
    // Exceptions which do not count as failures and thus not trigger the circuit breaker.
    private List<Class<? extends Throwable>> ignoredExceptions;

    private CircuitBreakerConfig(int maxFailures, int waitInterval, List<Class<? extends Throwable>> ignoredExceptions){
        this.maxFailures = maxFailures;
        this.waitInterval = waitInterval;
        this.ignoredExceptions = ignoredExceptions;
    }

    public Integer getMaxFailures() {
        return maxFailures;
    }

    public Integer getWaitInterval() {
        return waitInterval;
    }

    public List<Class<? extends Throwable>> getIgnoredExceptions() {
        return ignoredExceptions;
    }

    public static class Builder {
        private int maxFailures = 3;
        private int waitInterval = 60000;
        private List<Class<? extends Throwable>> ignoredExceptions = new ArrayList<>();

        public Builder maxFailures(int maxFailures) {
            this.maxFailures = maxFailures;
            return this;
        }

        public Builder waitInterval(int waitInterval) {
            this.waitInterval = waitInterval;
            return this;
        }

        public Builder ignoredException(Class<? extends Throwable> ignoredException) {
            ignoredExceptions.add(ignoredException);
            return this;
        }

        public Builder ignoredExceptions(List<Class<? extends Throwable>> ignoredExceptions) {
            this.ignoredExceptions = ignoredExceptions;
            return this;
        }

        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(maxFailures, waitInterval, ignoredExceptions);
        }
    }
}
