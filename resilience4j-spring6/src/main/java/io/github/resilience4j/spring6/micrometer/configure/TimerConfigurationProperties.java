package io.github.resilience4j.spring6.micrometer.configure;
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

import io.github.resilience4j.common.micrometer.configuration.CommonTimerConfigurationProperties;

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

/**
 * Main spring properties for timer configuration
 */
public class TimerConfigurationProperties extends CommonTimerConfigurationProperties {

    private int timerAspectOrder = LOWEST_PRECEDENCE;

    public int getTimerAspectOrder() {
        return timerAspectOrder;
    }

    /**
     * set timer aspect order
     *
     * @param timerAspectOrder timer aspect target order
     */
    public void setTimerAspectOrder(int timerAspectOrder) {
        this.timerAspectOrder = timerAspectOrder;
    }
}
