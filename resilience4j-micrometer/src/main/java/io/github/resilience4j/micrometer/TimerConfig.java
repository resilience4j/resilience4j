/*
 *
 *  Copyright 2023 Mariusz Kopylec
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.micrometer;

import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;

import java.util.StringJoiner;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

public class TimerConfig {

    private static final String DEFAULT_METRIC_NAMES = "resilience4j.timer.calls";
    private static final Function<Throwable, String> DEFAULT_ON_FAILURE_TAG_RESOLVER = throwable -> throwable.getClass().getSimpleName();

    @NonNull
    private final String metricNames;
    @NonNull
    private final Function<Throwable, String> onFailureTagResolver;

    private TimerConfig(@Nullable String metricNames, @Nullable Function<Throwable, String> onFailureTagResolver) {
        this.metricNames = requireNonNullElse(metricNames, DEFAULT_METRIC_NAMES);
        this.onFailureTagResolver = requireNonNullElse(onFailureTagResolver, DEFAULT_ON_FAILURE_TAG_RESOLVER);
    }

    /**
     * @return The metric names
     */
    public String getMetricNames() {
        return metricNames;
    }

    /**
     * @return The function that resolves a tag from the exception thrown from the decorated operation.
     */
    public Function<Throwable, String> getOnFailureTagResolver() {
        return onFailureTagResolver;
    }

    /**
     * Returns a builder to create a custom TimerConfig.
     *
     * @return a {@link Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Returns a builder to create a custom TimerConfig using specified config as prototype
     *
     * @param prototype A {@link TimerConfig} prototype.
     * @return a {@link Builder}
     */
    public static Builder from(TimerConfig prototype) {
        return new Builder(prototype);
    }

    /**
     * Creates a default Timer configuration.
     *
     * @return a default Timer configuration.
     */
    public static TimerConfig ofDefaults() {
        return new Builder().build();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TimerConfig.class.getSimpleName() + "[", "]")
                .add("metricNames=" + metricNames)
                .add("onFailureTagResolver=" + onFailureTagResolver)
                .toString();
    }

    /**
     * Builds Timer configuration
     */
    public static class Builder {

        @Nullable
        private String metricNames;
        @Nullable
        private Function<Throwable, String> onFailureTagResolver;

        private Builder() {
        }

        private Builder(@NonNull TimerConfig prototype) {
            requireNonNull(prototype, "Timer configuration prototype is null");
            metricNames = prototype.getMetricNames();
            onFailureTagResolver = prototype.getOnFailureTagResolver();
        }

        /**
         * @param metricNames The metric names.
         *                    Default is {@value DEFAULT_METRIC_NAMES}.
         * @return the TimerConfig.Builder
         */
        public Builder metricNames(@Nullable String metricNames) {
            this.metricNames = metricNames;
            return this;
        }

        /**
         * @param onFailureTagResolver A function that resolves a tag from the exception thrown from the decorated operation.
         *                             Default is exception class name.
         * @return the TimerConfig.Builder
         */
        public Builder onFailureTagResolver(@Nullable Function<Throwable, String> onFailureTagResolver) {
            this.onFailureTagResolver = onFailureTagResolver;
            return this;
        }

        /**
         * Builds a TimerConfig
         *
         * @return the TimerConfig
         */
        public TimerConfig build() {
            return new TimerConfig(metricNames, onFailureTagResolver);
        }
    }
}
