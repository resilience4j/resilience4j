/*
 *
 * Copyright 2023 Mariusz Kopylec
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
@file:Suppress("FunctionName")

package io.github.resilience4j.kotlin.micrometer

import io.github.resilience4j.micrometer.TimerConfig

/**
 * Creates new custom [TimerConfig].
 *
 * @param config methods of [TimerConfig.Builder] that customize resulting `TimerConfig`
 */
inline fun TimerConfig(
    config: TimerConfig.Builder.() -> Unit
): TimerConfig {
    return TimerConfig.custom().apply(config).build()
}

/**
 * Creates new custom [TimerConfig] based on [baseConfig].
 *
 * @param baseConfig base `TimerConfig`
 * @param config methods of [TimerConfig.Builder] that customize resulting `TimerConfig`
 */
inline fun TimerConfig(
    baseConfig: TimerConfig,
    config: TimerConfig.Builder.() -> Unit
): TimerConfig {
    return TimerConfig.from(baseConfig).apply(config).build()
}
