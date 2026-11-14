/*
 *
 *  Copyright 2026 Resilience4j contributors
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
package io.github.resilience4j.springboot3.scheduled.threadpool.autoconfigure;

import io.github.resilience4j.core.lang.NonNull;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;

@FunctionalInterface
public interface Resilience4JThreadPoolTaskSchedulerBuilderCustomizer {
    @NonNull ThreadPoolTaskSchedulerBuilder customize(@NonNull ThreadPoolTaskSchedulerBuilder builder);
}
