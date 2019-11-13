/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.decorators.configure;

import io.github.resilience4j.bulkhead.configure.BulkheadAspectHelper;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspectHelper;
import io.github.resilience4j.ratelimiter.configure.RateLimiterAspectHelper;
import io.github.resilience4j.retry.configure.*;
import io.github.resilience4j.utils.AspectJOnClasspathCondition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DecoratorsConfiguration {

    @Bean
    @Conditional(value = {AspectJOnClasspathCondition.class})
    public DecoratorsAspect decoratorsAspect(RetryAspectHelper retryAspectHelper,
            RateLimiterAspectHelper rateLimiterAspectHelper,
            BulkheadAspectHelper bulkheadAspectHelper,
            CircuitBreakerAspectHelper circuitBreakerAspectHelper,
            @Value("${resilience4j.decorators-aspect-order:2147483643}") int order) {
        return new DecoratorsAspect(
                retryAspectHelper, rateLimiterAspectHelper, bulkheadAspectHelper,
                circuitBreakerAspectHelper, order);
    }
}
