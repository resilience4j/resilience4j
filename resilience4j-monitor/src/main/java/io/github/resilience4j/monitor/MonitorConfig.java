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
package io.github.resilience4j.monitor;

import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.StringJoiner;
import java.util.function.Function;

import static io.github.resilience4j.monitor.LogLevel.DEBUG;
import static io.github.resilience4j.monitor.LogMode.SINGLE;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

public class MonitorConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 2781139368777959051L;

    private static final Function<?, String> DEFAULT_SUCCESS_RESULT_NAME_RESOLVER = result -> "unspecified";
    private static final Function<Throwable, String> DEFAULT_FAILURE_RESULT_NAME_RESOLVER = throwable -> throwable.getClass().getSimpleName();
    private static final LogMode DEFAULT_LOG_MODE = SINGLE;
    private static final LogLevel DEFAULT_LOG_LEVEL = DEBUG;

    @NonNull
    private final transient Function successResultNameResolver;
    @NonNull
    private final transient Function<Throwable, String> failureResultNameResolver;
    @NonNull
    private final LogMode logMode;
    @NonNull
    private final LogLevel logLevel;

    private MonitorConfig(@Nullable Function<?, String> successResultNameResolver,
                          @Nullable Function<Throwable, String> failureResultNameResolver,
                          @Nullable LogMode logMode,
                          @Nullable LogLevel logLevel) {
        this.successResultNameResolver = requireNonNullElse(successResultNameResolver, DEFAULT_SUCCESS_RESULT_NAME_RESOLVER);
        this.failureResultNameResolver = requireNonNullElse(failureResultNameResolver, DEFAULT_FAILURE_RESULT_NAME_RESOLVER);
        this.logMode = requireNonNullElse(logMode, DEFAULT_LOG_MODE);
        this.logLevel = requireNonNullElse(logLevel, DEFAULT_LOG_LEVEL);
    }

    /**
     * @param <T> The execution result type
     * @return The function that resolves a result name from the output returned from the execution.
     */
    @SuppressWarnings("unchecked")
    public <T> Function<T, String> getSuccessResultNameResolver() {
        return successResultNameResolver;
    }

    /**
     * @return The function that resolves a result name from the exception thrown from the execution.
     */
    public Function<Throwable, String> getFailureResultNameResolver() {
        return failureResultNameResolver;
    }

    /**
     * @return The log mode to use for logging the operation execution.
     */
    public LogMode getLogMode() {
        return logMode;
    }

    /**
     * @return The log level to use for logging the operation execution.
     */
    public LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * Returns a builder to create a custom MonitorConfig.
     *
     * @param <T> The execution result type
     * @return a {@link MonitorConfig.Builder}
     */
    public static <T> Builder<T> custom() {
        return new Builder<>();
    }

    /**
     * Returns a builder to create a custom MonitorConfig using specified config as prototype
     *
     * @param <T>       The execution result type
     * @param prototype A {@link MonitorConfig} prototype.
     * @return a {@link MonitorConfig.Builder}
     */
    public static <T> Builder<T> from(MonitorConfig prototype) {
        return new Builder<>(prototype);
    }

    /**
     * Creates a default Monitor configuration.
     *
     * @return a default Monitor configuration.
     */
    public static MonitorConfig ofDefaults() {
        return new Builder<>().build();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MonitorConfig.class.getSimpleName() + "[", "]")
                .add("successResultNameResolver=" + successResultNameResolver)
                .add("failureResultNameResolver=" + failureResultNameResolver)
                .add("logMode=" + logMode)
                .add("logLevel=" + logLevel)
                .toString();
    }

    public static class Builder<T> {

        @Nullable
        private Function<T, String> successResultNameResolver;
        @Nullable
        private Function<Throwable, String> failureResultNameResolver;
        @Nullable
        private LogMode logMode;
        @Nullable
        private LogLevel logLevel;

        private Builder() {
        }

        private Builder(@NonNull MonitorConfig prototype) {
            requireNonNull(prototype, "Monitor configuration prototype is null");
            successResultNameResolver = prototype.getSuccessResultNameResolver();
            failureResultNameResolver = prototype.getFailureResultNameResolver();
            logMode = prototype.getLogMode();
            logLevel = prototype.getLogLevel();
        }

        /**
         * @param successResultNameResolver A function that resolves a result name from the output returned from the execution.
         *                                  Default is "unspecified".
         * @return the MonitorConfig.Builder
         */
        public Builder<T> successResultNameResolver(@Nullable Function<T, String> successResultNameResolver) {
            this.successResultNameResolver = successResultNameResolver;
            return this;
        }

        /**
         * @param failureResultNameResolver A function that resolves a result name from the exception thrown from the execution.
         *                                  Default is exception class name.
         * @return the MonitorConfig.Builder
         */
        public Builder<T> failureResultNameResolver(@Nullable Function<Throwable, String> failureResultNameResolver) {
            this.failureResultNameResolver = failureResultNameResolver;
            return this;
        }

        /**
         * @param logMode The log mode to use for logging the operation execution.
         *                Default is @{@link LogMode#SINGLE}
         * @return the MonitorConfig.Builder
         */
        public Builder<T> logMode(@Nullable LogMode logMode) {
            this.logMode = logMode;
            return this;
        }

        /**
         * @param logLevel The log level to use for logging the operation execution.
         *                 Default is @{@link LogLevel#DEBUG}
         * @return the MonitorConfig.Builder
         */
        public Builder<T> logLevel(@Nullable LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Builds a MonitorConfig
         *
         * @return the MonitorConfig
         */
        public MonitorConfig build() {
            return new MonitorConfig(successResultNameResolver, failureResultNameResolver, logMode, logLevel);
        }
    }
}
