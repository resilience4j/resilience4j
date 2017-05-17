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

package io.github.resilience4j.ratpack.retry;

public class RetryConfig {

    private boolean defaults = false;
    private int maxAttempts;
    private int waitDurationInMillis;

    public RetryConfig defaults(boolean defaults) {
        this.defaults = defaults;
        return this;
    }

    public RetryConfig maxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public RetryConfig waitDurationInMillis(int waitDurationInMillis) {
        this.waitDurationInMillis = waitDurationInMillis;
        return this;
    }

    public boolean getDefaults() {
        return defaults;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getWaitDurationInMillis() {
        return waitDurationInMillis;
    }
}
