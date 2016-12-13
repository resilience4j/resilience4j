/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.robwin.circuitbreaker;

import io.github.robwin.circuitbreaker.internal.RingBitSet;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.All)
public class RingBitSetBenchmark {

    private static final int CAPACITY = 1000;
    private static final int ITERATION_COUNT = 10;
    private static final int WARMUP_COUNT = 10;
    private static final int THREAD_COUNT = 2;
    private static final int FORK_COUNT = 2;

    private RingBitSet ringBitSet;

    @Setup
    public void setUp() {
        ringBitSet = new RingBitSet(CAPACITY);
    }

    @Benchmark
    @Fork(value = FORK_COUNT)
    @Group("ringBitSet")
    @GroupThreads(THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public void concurrentSetBits(Blackhole bh) {
        int firstCardinality = ringBitSet.setNextBit(true);
        bh.consume(firstCardinality);
        int secondCardinality = ringBitSet.setNextBit(false);
        bh.consume(secondCardinality);
    }

    @Benchmark
    @Fork(value = FORK_COUNT)
    @Group("ringBitSet")
    @GroupThreads(1)
    @Warmup(iterations = WARMUP_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public void concurrentCardinality(Blackhole bh) {
        int cardinality = ringBitSet.cardinality();
        bh.consume(cardinality);
    }
}
