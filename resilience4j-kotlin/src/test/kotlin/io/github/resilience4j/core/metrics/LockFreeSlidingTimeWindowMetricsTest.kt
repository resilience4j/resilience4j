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
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import java.util.concurrent.TimeUnit

/**
 * Tests the [LockFreeSlidingTimeWindowMetrics] class using the Lincheck framework.
 *
 * Since Lincheck does not account for time, advancing the clock is modelled as an operation of its own.
 * Each test instance owns its [ManualClock], so the sequential execution that Lincheck uses to validate
 * the results observes exactly the same time as the concurrent one. The window is kept small, so it slides
 * and old records are discarded within a single scenario.
 *
 * The time advances by at most one whole second per operation:
 * - A time slice starts at the time the window was advanced to it, so with sub-second steps the slice boundaries
 *   depend on when an operation happened to read the clock, and a record can be discarded up to one slice earlier
 *   in the concurrent execution than in the sequential one. Whole seconds keep the slices aligned in both.
 * - The window catches up with a larger jump one time slice at a time, so a concurrent snapshot can observe it at
 *   a second in between. That is fine for a real clock, but not for a jump that is a single atomic operation.
 *
 * The model checking test is ignored because it is extremely slow (~3m) and is most useful during development.
 * The metrics classes are excluded from the Jacoco instrumentation of this module, as it interferes with model checking.
 *
 * In contrast, stress testing is faster and can catch rare bugs that require many context switches,
 * making it suitable for regular builds.
 */

class LockFreeSlidingTimeWindowMetricsTest {
    private val clock = ManualClock()
    private val metrics = LockFreeSlidingTimeWindowMetrics(3, clock)

    @Operation
    fun advanceTime(@Param(gen = IntGen::class, conf = "0:1") seconds: Int) =
        clock.advanceBySeconds(seconds.toLong())

    @Operation
    fun record(duration: Long, durationUnit: TimeUnit, outcome: Outcome): Snapshot =
        metrics.record(duration, durationUnit, outcome)

    @Operation
    fun getSnapshot(): Snapshot = metrics.snapshot

    @Test
    fun stressTest() = StressOptions().check(this::class)

    @Test
    @Disabled
    fun modelCheckingTest() = ModelCheckingOptions().check(this::class)

    /**
     * A thread that read the tail before a full window advance, and the clock after it, must not append
     * a time slice that is less than a time slice after the tail. Otherwise the record of the other thread
     * is discarded too early, together with the time slice it was recorded in.
     */
    @Test
    fun staleWindowAdvanceDoesNotLoseRecords() = ModelCheckingOptions()
        .iterations(0)
        .addCustomScenario {
            initial {
                repeat(3) { actor(LockFreeSlidingTimeWindowMetricsTest::advanceTime, 1) }
            }
            parallel {
                thread {
                    actor(LockFreeSlidingTimeWindowMetricsTest::record, 1L, TimeUnit.MILLISECONDS, Outcome.SUCCESS)
                    repeat(2) { actor(LockFreeSlidingTimeWindowMetricsTest::advanceTime, 1) }
                }
                thread {
                    actor(LockFreeSlidingTimeWindowMetricsTest::record, 1L, TimeUnit.MILLISECONDS, Outcome.SUCCESS)
                }
            }
        }
        .check(this::class)
}
