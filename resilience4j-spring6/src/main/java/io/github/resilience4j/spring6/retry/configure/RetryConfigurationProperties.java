package io.github.resilience4j.spring6.retry.configure;
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

import org.springframework.core.Ordered;

/**
 * Main spring properties for retry configuration
 */
public class RetryConfigurationProperties extends
    io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties {

    private int retryAspectOrder = Ordered.LOWEST_PRECEDENCE - 5;

    /**
     * As of release 0.16.0 as we set an implicit spring aspect order now which is retry then
     * circuit breaker then rate limiter then bulkhead but the user can override it still if he has
     * different use case but bulkhead will be first aspect all the time due to the implicit order
     * we have it for bulkhead
     */
    public int getRetryAspectOrder() {
        return retryAspectOrder;
    }

    /**
     * set retry aspect order
     *
     * @param retryAspectOrder retry aspect target order
     */
    public void setRetryAspectOrder(int retryAspectOrder) {
        this.retryAspectOrder = retryAspectOrder;
    }

}
