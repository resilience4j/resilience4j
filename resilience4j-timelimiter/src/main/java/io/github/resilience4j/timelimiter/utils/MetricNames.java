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

package io.github.resilience4j.timelimiter.utils;

public final class MetricNames {

    public static final String DEFAULT_PREFIX = "resilience4j.timelimiter";
    public static final String SUCCESSFUL = "successful";
    public static final String FAILED = "failed";
    public static final String TIMEOUT = "timeout";
    public static final String PREFIX_NULL = "Prefix must not be null";
    public static final String ITERABLE_NULL = "TimeLimiters iterable must not be null";
    private MetricNames() {
    }

}
