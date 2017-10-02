/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.autoconfigure;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "resilience4j.ratelimiter")
@Component
public class RateLimiterProperties {
    // This property gives you control over RateLimiter aspect application order.
    // Integer.MAX_VALUE means that RateLimiter aspect will be last one applied to any decorated bean.
    // It also means that by default RateLimiter will be executed AFTER CircuitBreaker.
    // Be adjusting RateLimiterProperties.rateLimiterAspectOrder and CircuitBreakerProperties.circuitBreakerAspectOrder
    // you explicitly define aspects CircuitBreaker and RateLimiter execution sequence.
    private int rateLimiterAspectOrder = Integer.MAX_VALUE;
    private Map<String, LimiterProperties> limiters = new HashMap<>();

    private LimiterProperties getLimiterProperties(String limiter) {
        return limiters.get(limiter);
    }

    public RateLimiterConfig createRateLimiterConfig(String limiter) {
        return createRateLimiterConfig(getLimiterProperties(limiter));
    }

    public int getRateLimiterAspectOrder() {
        return rateLimiterAspectOrder;
    }

    public void setRateLimiterAspectOrder(int rateLimiterAspectOrder) {
        this.rateLimiterAspectOrder = rateLimiterAspectOrder;
    }

    public static RateLimiterConfig createRateLimiterConfig(LimiterProperties limiterProperties) {
        if (limiterProperties == null) {
            return RateLimiterConfig.ofDefaults();
        }

        RateLimiterConfig.Builder rateLimiterConfigBuilder = RateLimiterConfig.custom();

        if (limiterProperties.getLimitForPeriod() != null) {
            rateLimiterConfigBuilder.limitForPeriod(limiterProperties.getLimitForPeriod());
        }

        if (limiterProperties.getLimitRefreshPeriodInMillis() != null) {
            rateLimiterConfigBuilder.limitRefreshPeriod(Duration.ofMillis(limiterProperties.getLimitRefreshPeriodInMillis()));
        }

        if (limiterProperties.getTimeoutInMillis() != null) {
            rateLimiterConfigBuilder.timeoutDuration(Duration.ofMillis(limiterProperties.getTimeoutInMillis()));
        }

        return rateLimiterConfigBuilder.build();
    }

    public Map<String, LimiterProperties> getLimiters() {
        return limiters;
    }

    /**
     * Class storing property values for configuring {@link io.github.resilience4j.ratelimiter.RateLimiterConfig} instances.
     */
    public static class LimiterProperties {

        private Integer limitForPeriod;
        private Integer limitRefreshPeriodInMillis;
        private Integer timeoutInMillis;
        private Boolean subscribeForEvents = false;
        private Boolean registerHealthIndicator = false;
        private Integer eventConsumerBufferSize = 100;

        /**
         * Configures the permissions limit for refresh period.
         * Count of permissions available during one rate limiter period
         * specified by {@link RateLimiterConfig#limitRefreshPeriod} value.
         * Default value is 50.
         *
         * @return the permissions limit for refresh period
         */
        public Integer getLimitForPeriod() {
            return limitForPeriod;
        }

        /**
         * Configures the permissions limit for refresh period.
         * Count of permissions available during one rate limiter period
         * specified by {@link RateLimiterConfig#limitRefreshPeriod} value.
         * Default value is 50.
         *
         * @param limitForPeriod the permissions limit for refresh period
         */
        public void setLimitForPeriod(Integer limitForPeriod) {
            this.limitForPeriod = limitForPeriod;
        }

        /**
         * Configures the period of limit refresh.
         * After each period rate limiter sets its permissions
         * count to {@link RateLimiterConfig#limitForPeriod} value.
         * Default value is 500 nanoseconds.
         *
         * @return the period of limit refresh
         */
        public Integer getLimitRefreshPeriodInMillis() {
            return limitRefreshPeriodInMillis;
        }

        /**
         * Configures the period of limit refresh.
         * After each period rate limiter sets its permissions
         * count to {@link RateLimiterConfig#limitForPeriod} value.
         * Default value is 500 nanoseconds.
         *
         * @param limitRefreshPeriodInMillis the period of limit refresh
         */
        public void setLimitRefreshPeriodInMillis(Integer limitRefreshPeriodInMillis) {
            this.limitRefreshPeriodInMillis = limitRefreshPeriodInMillis;
        }

        /**
         * Configures the default wait for permission duration.
         * Default value is 5 seconds.
         *
         * @return wait for permission duration
         */
        public Integer getTimeoutInMillis() {
            return timeoutInMillis;
        }

        /**
         * Configures the default wait for permission duration.
         * Default value is 5 seconds.
         *
         * @param timeoutInMillis wait for permission duration
         */
        public void setTimeoutInMillis(Integer timeoutInMillis) {
            this.timeoutInMillis = timeoutInMillis;
        }

        public Boolean getSubscribeForEvents() {
            return subscribeForEvents;
        }

        public void setSubscribeForEvents(Boolean subscribeForEvents) {
            this.subscribeForEvents = subscribeForEvents;
        }

        public Integer getEventConsumerBufferSize() {
            return eventConsumerBufferSize;
        }

        public void setEventConsumerBufferSize(Integer eventConsumerBufferSize) {
            this.eventConsumerBufferSize = eventConsumerBufferSize;
        }

        public Boolean getRegisterHealthIndicator() {
            return registerHealthIndicator;
        }

        public void setRegisterHealthIndicator(Boolean registerHealthIndicator) {
            this.registerHealthIndicator = registerHealthIndicator;
        }
    }

}
