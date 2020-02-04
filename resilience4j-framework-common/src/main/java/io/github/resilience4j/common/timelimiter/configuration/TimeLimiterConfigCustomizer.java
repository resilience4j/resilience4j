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
package io.github.resilience4j.common.timelimiter.configuration;

import io.github.resilience4j.common.CustomizerWithName;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.util.function.Consumer;

/**
 * Enable customization time limiter configuration builders programmatically.
 */
public interface TimeLimiterConfigCustomizer extends CustomizerWithName {

    /**
     * Customize time limiter configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(TimeLimiterConfig.Builder configBuilder);

    /**
     * A convenient method to create TimeLimiterConfigCustomizer using {@link Consumer}
     *
     * @param instanceName the name of the instance
     * @param consumer     delegate call to Consumer when  {@link TimeLimiterConfigCustomizer#customize(TimeLimiterConfig.Builder)}
     *                     is called
     * @return Customizer instance
     */
    static TimeLimiterConfigCustomizer of(@NonNull String instanceName,
        @NonNull Consumer<TimeLimiterConfig.Builder> consumer) {
        return new TimeLimiterConfigCustomizer() {

            @Override
            public void customize(TimeLimiterConfig.Builder builder) {
                consumer.accept(builder);
            }

            @Override
            public String name() {
                return instanceName;
            }
        };
    }

}
