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
package io.github.resilience4j.circularbuffer;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author bstorozhuk
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class CircularBufferBenchmark {

    public static final int FORK_COUNT = 2;
    private static final int WARMUP_COUNT = 10;
    private static final int ITERATION_COUNT = 10;
    private static final int CAPACITY = 10;
    private CircularFifoBuffer<Object> circularFifoBuffer;
    private Object event;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(".*" + CircularBufferBenchmark.class.getSimpleName() + ".*")
            .build();
        new Runner(options).run();
    }

    @Setup
    public void setUp() {
        event = new Object();
        circularFifoBuffer = new ConcurrentCircularFifoBuffer<>(CAPACITY);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("circularBuffer")
    @GroupThreads(1)
    public void circularBufferAddEvent() {
        circularFifoBuffer.add(event);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("circularBuffer")
    @GroupThreads(1)
    public void circularBufferToList(Blackhole bh) {
        List<Object> events = circularFifoBuffer.toList();
        bh.consume(events);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("circularBuffer")
    @GroupThreads(1)
    public void circularBufferSize(Blackhole bh) {
        int size = circularFifoBuffer.size();
        bh.consume(size);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("circularBuffer")
    @GroupThreads(1)
    public void circularBufferTakeEvent(Blackhole bh) {
        Optional<Object> event = circularFifoBuffer.take();
        bh.consume(event);
    }
}
