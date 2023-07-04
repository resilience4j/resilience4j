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
    private static final Function<?, String> DEFAULT_SUCCESS_RESULT_NAME_RESOLVER = result -> "none";
    private static final Function<Throwable, String> DEFAULT_FAILURE_RESULT_NAME_RESOLVER = throwable -> throwable.getClass().getSimpleName();

    @NonNull
    private final String metricNames;
    @NonNull
    private final Function successResultNameResolver;
    @NonNull
    private final Function<Throwable, String> failureResultNameResolver;

    private TimerConfig(@Nullable String metricNames, @Nullable Function<?, String> successResultNameResolver, @Nullable Function<Throwable, String> failureResultNameResolver) {
        this.metricNames = requireNonNullElse(metricNames, DEFAULT_METRIC_NAMES);
        this.successResultNameResolver = requireNonNullElse(successResultNameResolver, DEFAULT_SUCCESS_RESULT_NAME_RESOLVER);
        this.failureResultNameResolver = requireNonNullElse(failureResultNameResolver, DEFAULT_FAILURE_RESULT_NAME_RESOLVER);
    }

    /**
     * @return The metric names
     */
    public String getMetricNames() {
        return metricNames;
    }

    /**
     * @param <T> The decorated operation result type
     * @return The function that resolves a result name from the output returned from the decorated operation.
     */
    @SuppressWarnings("unchecked")
    public <T> Function<T, String> getSuccessResultNameResolver() {
        return successResultNameResolver;
    }

    /**
     * @return The function that resolves a result name from the exception thrown from the decorated operation.
     */
    public Function<Throwable, String> getFailureResultNameResolver() {
        return failureResultNameResolver;
    }

    /**
     * Returns a builder to create a custom TimerConfig.
     *
     * @param <T> The decorated operation result type
     * @return a {@link Builder}
     */
    public static <T> Builder<T> custom() {
        return new Builder<>();
    }

    /**
     * Returns a builder to create a custom TimerConfig using specified config as prototype
     *
     * @param <T>       The decorated operation result type
     * @param prototype A {@link TimerConfig} prototype.
     * @return a {@link Builder}
     */
    public static <T> Builder<T> from(TimerConfig prototype) {
        return new Builder<>(prototype);
    }

    /**
     * Creates a default Timer configuration.
     *
     * @return a default Timer configuration.
     */
    public static TimerConfig ofDefaults() {
        return new Builder<>().build();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TimerConfig.class.getSimpleName() + "[", "]")
                .add("metricNames=" + metricNames)
                .add("successResultNameResolver=" + successResultNameResolver)
                .add("failureResultNameResolver=" + failureResultNameResolver)
                .toString();
    }

    public static class Builder<T> {

        @Nullable
        private String metricNames;
        @Nullable
        private Function<T, String> successResultNameResolver;
        @Nullable
        private Function<Throwable, String> failureResultNameResolver;

        private Builder() {
        }

        private Builder(@NonNull TimerConfig prototype) {
            requireNonNull(prototype, "Timer configuration prototype is null");
            metricNames = prototype.getMetricNames();
            successResultNameResolver = prototype.getSuccessResultNameResolver();
            failureResultNameResolver = prototype.getFailureResultNameResolver();
        }

        /**
         * @param metricNames The metric names.
         *                    Default is "resilience4j.timer.calls".
         * @return the TimerConfig.Builder
         */
        public Builder<T> metricNames(@Nullable String metricNames) {
            this.metricNames = metricNames;
            return this;
        }

        /**
         * @param successResultNameResolver A function that resolves a result name from the output returned from the decorated operation.
         *                                  Default is "unspecified".
         * @return the TimerConfig.Builder
         */
        public Builder<T> successResultNameResolver(@Nullable Function<T, String> successResultNameResolver) {
            this.successResultNameResolver = successResultNameResolver;
            return this;
        }

        /**
         * @param failureResultNameResolver A function that resolves a result name from the exception thrown from the decorated operation.
         *                                  Default is exception class name.
         * @return the TimerConfig.Builder
         */
        public Builder<T> failureResultNameResolver(@Nullable Function<Throwable, String> failureResultNameResolver) {
            this.failureResultNameResolver = failureResultNameResolver;
            return this;
        }

        /**
         * Builds a TimerConfig
         *
         * @return the TimerConfig
         */
        public TimerConfig build() {
            return new TimerConfig(metricNames, successResultNameResolver, failureResultNameResolver);
        }
    }
}
