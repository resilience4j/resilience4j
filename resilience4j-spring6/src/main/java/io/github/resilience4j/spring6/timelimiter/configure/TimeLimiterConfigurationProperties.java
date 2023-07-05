/*
 * Copyright 2020 Ingyu Hwang
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

package io.github.resilience4j.spring6.timelimiter.configure;

import org.springframework.core.Ordered;

@SuppressWarnings("squid:S2176")
public class TimeLimiterConfigurationProperties extends
    io.github.resilience4j.common.timelimiter.configuration.CommonTimeLimiterConfigurationProperties {

    private int timeLimiterAspectOrder = Ordered.LOWEST_PRECEDENCE - 2;

    public int getTimeLimiterAspectOrder() {
        return timeLimiterAspectOrder;
    }

    /**
     * set timeLimiter aspect order
     *
     * @param timeLimiterAspectOrder timeLimiter aspect target order
     */
    public void setTimeLimiterAspectOrder(int timeLimiterAspectOrder) {
        this.timeLimiterAspectOrder = timeLimiterAspectOrder;
    }

}
