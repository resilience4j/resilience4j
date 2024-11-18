/*
 *
 *  Copyright 2024 Florentin Simion and Rares Vlasceanu
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
package io.github.resilience4j.core.metrics

import io.github.resilience4j.core.metrics.Metrics.Outcome
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.categories.Category
import java.util.concurrent.TimeUnit

/**
 * Tests the [LockFreeFixedSizeSlidingWindowMetrics] class using the Lincheck framework.
 *
 * The model checking test is ignored because it is extremly slow (~3m) and is most useful during development. Also,
 * to run it you need to disable the Jacoco plugin, as it seems to interfere with the model checking test.
 *
 * In contrast, stress testing is faster and can catch rare bugs that require many context switches,
 * making it suitable for regular builds.
 */
@Category(ConcurrencyTests::class)
class LockFreeFixedSizeSlidingWindowMetricsTest {
    private val metrics = LockFreeFixedSizeSlidingWindowMetrics(5)

    @Operation
    fun record(duration: Long, durationUnit: TimeUnit, outcome: Outcome): Snapshot =
        metrics.record(duration, durationUnit, outcome)

    @Operation
    fun getSnapshot(): Snapshot = metrics.snapshot

    @Test
    fun stressTest() = StressOptions().check(this::class)

    @Test
    @Ignore
    fun modelCheckingTest() = ModelCheckingOptions().check(this::class)
}
