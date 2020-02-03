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

package io.github.resilience4j.timelimiter.autoconfigure;

import io.github.resilience4j.common.IntegerToDurationConverter;
import io.github.resilience4j.common.StringToDurationConverter;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({IntegerToDurationConverter.class, StringToDurationConverter.class})
public class TimeLimiterConfigurationOnMissingBean extends AbstractTimeLimiterConfigurationOnMissingBean {

    @Bean
    public EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry() {
        return timeLimiterConfiguration.timeLimiterEventsConsumerRegistry();
    }

}
