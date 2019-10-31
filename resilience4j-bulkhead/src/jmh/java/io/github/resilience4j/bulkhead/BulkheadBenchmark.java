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

import io.github.resilience4j.adapter.RxJava2Adapter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

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

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .threads(2)
            .addProfiler(GCProfiler.class)
            .build();
        new Runner(options).run();
    }

    @Setup
    public void setUp() {
        stringSupplier = () -> {
            Blackhole.consumeCPU(100);
            return "Hello Benchmark";
        };

        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(2)
            .build();

        Bulkhead bulkhead = Bulkhead.of("test", config);
        protectedSupplier = Bulkhead.decorateSupplier(bulkhead, stringSupplier);

        Bulkhead bulkheadWithSubscriber = Bulkhead.of("test-with-subscriber", config);
        RxJava2Adapter.toFlowable(bulkheadWithSubscriber.getEventPublisher()).subscribe();
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