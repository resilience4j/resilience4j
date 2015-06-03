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

public class CircuitBreakerConfig {
    private int maxFailures;

    private int waitInterval;

    private CircuitBreakerConfig(int maxFailures, int waitInterval){
        this.maxFailures = maxFailures;
        this.waitInterval = waitInterval;
    }

    public Integer getMaxFailures() {
        return maxFailures;
    }

    public Integer getWaitInterval() {
        return waitInterval;
    }

    public static class Builder {
        private int maxFailures = 3;
        private int waitInterval = 60000;

        public Builder maxFailures(int maxFailures) {
            this.maxFailures = maxFailures;
            return this;
        }

        public Builder waitInterval(int waitInterval) {
            this.waitInterval = waitInterval;
            return this;
        }

        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(maxFailures, waitInterval);
        }
    }
}
