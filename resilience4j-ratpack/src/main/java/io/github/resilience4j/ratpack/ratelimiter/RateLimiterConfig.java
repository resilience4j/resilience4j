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

package io.github.resilience4j.ratpack.ratelimiter;

public class RateLimiterConfig {

    private boolean defaults = false;
    private Integer limitForPeriod = 50;
    private Integer limitRefreshPeriodInNanos = 500;
    private Integer timeoutInMillis = 5000;
    private Integer eventConsumerBufferSize = 100;

    public RateLimiterConfig defaults(boolean defaults) {
        this.defaults = defaults;
        return this;
    }

    public RateLimiterConfig limitForPeriod(Integer limitForPeriod) {
        this.limitForPeriod = limitForPeriod;
        return this;
    }

    public RateLimiterConfig limitRefreshPeriodInNanos(Integer limitRefreshPeriodInNanos) {
        this.limitRefreshPeriodInNanos = limitRefreshPeriodInNanos;
        return this;
    }

    public RateLimiterConfig timeoutInMillis(Integer timeoutInMillis) {
        this.timeoutInMillis = timeoutInMillis;
        return this;
    }

    public RateLimiterConfig eventConsumerBufferSize(Integer eventConsumerBufferSize) {
        this.eventConsumerBufferSize = eventConsumerBufferSize;
        return this;
    }

    public Boolean getDefaults() {
        return defaults;
    }

    public Integer getLimitForPeriod() {
        return limitForPeriod;
    }

    public Integer getLimitRefreshPeriodInNanos() {
        return limitRefreshPeriodInNanos;
    }

    public Integer getTimeoutInMillis() {
        return timeoutInMillis;
    }

    public Integer getEventConsumerBufferSize() {
        return eventConsumerBufferSize;
    }

}
