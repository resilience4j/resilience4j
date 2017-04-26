/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
public class BulkheadBenchmark {

    private static final int ITERATION_COUNT = 10;
    private static final int WARMUP_COUNT = 10;
    private static final int THREAD_COUNT = 2;
    private static final int FORK_COUNT = 2;

    private Supplier<String> protectedSupplier;
    private Supplier<String> protectedSupplierWithSb;
    private Supplier<String> stringSupplier;

    @Setup
    public void setUp() {
        stringSupplier = () -> {
            Blackhole.consumeCPU(100);
            return "Hello Benchmark";
        };

        Bulkhead bulkhead = Bulkhead.of("test", 2);
        protectedSupplier = Bulkhead.decorateSupplier(bulkhead, stringSupplier);

        Bulkhead bulkheadWithSubscriber = Bulkhead.of("test-with-subscriber", 2);
        bulkheadWithSubscriber.getEventStream().subscribe();
        protectedSupplierWithSb = Bulkhead.decorateSupplier(bulkheadWithSubscriber, stringSupplier);
    }

    @Benchmark
    @Fork(value = FORK_COUNT)
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public String directSupplier() {
        return stringSupplier.get();
    }

    @Benchmark
    @Fork(value = FORK_COUNT)
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public String protectedSupplier() {
        return protectedSupplier.get();
    }

    @Benchmark
    @Fork(value = FORK_COUNT)
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public String protectedSupplierWithSubscriber() {
        return protectedSupplierWithSb.get();
    }
}