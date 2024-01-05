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
package io.github.resilience4j.common.micrometer.configuration;

import io.github.resilience4j.common.CustomizerWithName;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.micrometer.TimerConfig;

import java.util.function.Consumer;

/**
 * Timer configuration customizer.
 */
public interface TimerConfigCustomizer extends CustomizerWithName {

    /**
     * Timer configuration builder.
     *
     * @param configBuilder to be customized
     */
    void customize(TimerConfig.Builder configBuilder);

    /**
     * A convenient method to create TimerConfigCustomizer using {@link Consumer}
     *
     * @param instanceName the name of the instance
     * @param consumer     delegate call to Consumer when  {@link TimerConfigCustomizer#customize(TimerConfig.Builder)}
     *                     is called
     * @return Customizer instance
     */
    static TimerConfigCustomizer of(@NonNull String instanceName, @NonNull Consumer<TimerConfig.Builder> consumer) {
        return new TimerConfigCustomizer() {

            @Override
            public void customize(TimerConfig.Builder builder) {
                consumer.accept(builder);
            }

            @Override
            public String name() {
                return instanceName;
            }
        };
    }
}
