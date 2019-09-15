/*
 * Copyright 2019 Ingyu Hwang
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

package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.metrics.AbstractTimeLimiterMetricsTest;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

public class TimeLimiterMetricsPublisherTest extends AbstractTimeLimiterMetricsTest {

    @Override
    protected TimeLimiter given(String prefix, MetricRegistry metricRegistry) {
        TimeLimiterRegistry timeLimiterRegistry =
                TimeLimiterRegistry.of(TimeLimiterConfig.ofDefaults(), new TimeLimiterMetricsPublisher(prefix, metricRegistry));

        return timeLimiterRegistry.timeLimiter("testLimit");
    }

    @Override
    protected TimeLimiter given(MetricRegistry metricRegistry) {
        TimeLimiterRegistry timeLimiterRegistry =
                TimeLimiterRegistry.of(TimeLimiterConfig.ofDefaults(), new TimeLimiterMetricsPublisher(metricRegistry));

        return timeLimiterRegistry.timeLimiter("testLimit");
    }
}