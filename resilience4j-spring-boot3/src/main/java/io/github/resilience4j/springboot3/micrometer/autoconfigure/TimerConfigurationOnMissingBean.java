/*
 * Copyright 2023 Mariusz Kopylec
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

package io.github.resilience4j.springboot3.micrometer.autoconfigure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.micrometer.event.TimerEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimerConfigurationOnMissingBean extends AbstractTimerConfigurationOnMissingBean {

    /**
     * The EventConsumerRegistry is used to manage EventConsumer instances.
     * The EventConsumerRegistry is used by the Timer events monitor to show the latest TimerEvent events
     * for each Timer instance.
     *
     * @return a default EventConsumerRegistry {@link DefaultEventConsumerRegistry}
     */
    @Bean
    @ConditionalOnMissingBean(value = TimerEvent.class, parameterizedContainer = EventConsumerRegistry.class)
    public EventConsumerRegistry<TimerEvent> timerEventsConsumerRegistry() {
        return timerConfiguration.timerEventsConsumerRegistry();
    }
}
