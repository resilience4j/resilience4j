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

import java.util.concurrent.TimeUnit;

/**
 * @author bstorozhuk
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class ConcurrentEvictingQueueBenchmark {

    public static final int FORK_COUNT = 2;
    private static final int WARMUP_COUNT = 10;
    private static final int ITERATION_COUNT = 10;
    private static final int CAPACITY = 10;
    private ConcurrentEvictingQueue<Object> queue;
    private Object event;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(".*" + ConcurrentEvictingQueueBenchmark.class.getSimpleName() + ".*")
            .build();
        new Runner(options).run();
    }

    @Setup
    public void setUp() {
        event = new Object();
        queue = new ConcurrentEvictingQueue<>(CAPACITY);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("concurrentEvictingQueue")
    @GroupThreads(2)
    public void concurrentEvictingQueueAdd() {
        queue.add(event);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("concurrentEvictingQueue")
    @GroupThreads(1)
    public void concurrentEvictingQueueSize(Blackhole bh) {
        int size = queue.size();
        bh.consume(size);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("concurrentEvictingQueue")
    @GroupThreads(1)
    public void concurrentEvictingQueuePoll(Blackhole bh) {
        Object event = queue.poll();
        bh.consume(event);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("concurrentEvictingQueue")
    @GroupThreads(1)
    public void concurrentEvictingQueuePeek(Blackhole bh) {
        Object event = queue.peek();
        bh.consume(event);
    }
}
