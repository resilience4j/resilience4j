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

import io.github.resilience4j.core.ManualClock
import io.github.resilience4j.core.metrics.Metrics.Outcome
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.junit.AfterClass
import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.categories.Category
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Tests the [LockFreeSlidingTimeWindowMetrics] class using the Lincheck framework.
 *
 * Since Lincheck does not account for time, we need to manually and quickly advance the time to ensure
 * the test validates the sliding behavior of the time window (i.e., sliding the window every second).
 * We achieve this by using a static timer that advances the time every 10 ms by a value between 1s and 1.25s
 *
 * It is important to note that the windowSize must be large enough to prevent records from being discarded.
 * This is because the sequential version may run faster or slower than the concurrent version,
 * leading to different results and potential test failures, even if there is no bug in the implementation.
 * This can be verified by commenting out the line where metrics are discarded, which should make the test pass.
 * The required window size depends on the machine's speed, so slower machines may need a larger size.
 *
 * The model checking test is ignored because it is extremely slow (~3m) and is most useful during development. Also,
 * to run it you need to disable the Jacoco plugin, as it seems to interfere with the model checking test.
 *
 * In contrast, stress testing is faster and can catch rare bugs that require many context switches,
 * making it suitable for regular builds.
 */
@Category(ConcurrencyTests::class)
class LockFreeSlidingTimeWindowMetricsTest {
    companion object {
        private val clock = ManualClock()
        private val scheduler = Executors.newSingleThreadScheduledExecutor().apply {
            val interval = 10_000_000L

            scheduleAtFixedRate({
                clock.advanceByMillis(1000 + Random.nextLong(0, 250))
            }, interval, interval, TimeUnit.NANOSECONDS)
        }

        @AfterClass
        @JvmStatic
        fun cleanUp() {
            scheduler.shutdownNow()
        }
    }

    private val metrics = LockFreeSlidingTimeWindowMetrics(10, clock)

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
