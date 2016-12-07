package io.github.robwin.circuitbreaker;

import io.github.robwin.circuitbreaker.internal.CircularFifoBuffer;
import io.github.robwin.circuitbreaker.internal.CircularFifoBufferTest;
import io.github.robwin.circuitbreaker.internal.ConcurrentCircularBuffer;
import javaslang.collection.List;
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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author bstorozhuk
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
public class CircularBufferBenchmark {
    public static final int FORK_COUNT = 2;
    private static final int WARMUP_COUNT = 10;
    private static final int ITERATION_COUNT = 10;
    private static final int THREAD_COUNT = 2;
    private static final int CAPACITY = 100;
    private CircularFifoBuffer<Object> circularFifoBuffer;
    private ConcurrentCircularBuffer<Object> concurrentCircularBuffer;
    private Object event;
    private Object[] buffer;

    @Setup
    public void setUp() {
        event = new Object();
        circularFifoBuffer = new CircularFifoBuffer<>(CAPACITY);
        concurrentCircularBuffer = new ConcurrentCircularBuffer<>(CAPACITY);
        buffer = new Object[CAPACITY];
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("circularBuffer")
    @GroupThreads(THREAD_COUNT)
    public void circularBufferWriter() {
        circularFifoBuffer.add(event);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("circularBuffer")
    @GroupThreads(1)
    public void circularBufferReaderMonitoring(Blackhole bh) {
        int size = circularFifoBuffer.size();
        bh.consume(size);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("circularBuffer")
    @GroupThreads(1)
    public void circularBufferReaderEvents(Blackhole bh) {
        List<Object> events = circularFifoBuffer.toList();
        bh.consume(events);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("concurrentBuffer")
    @GroupThreads(THREAD_COUNT)
    public void concurrentBufferWriter() {
        concurrentCircularBuffer.add(event);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("concurrentBuffer")
    @GroupThreads(1)
    public void concurrentBufferReaderMonitoring(Blackhole bh) {
        int size = concurrentCircularBuffer.size();
        bh.consume(size);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Group("concurrentBuffer")
    @GroupThreads(1)
    public void concurrentBufferReaderEvents(Blackhole bh) {
        Object[] eventsArray = concurrentCircularBuffer.toArray(buffer);
        List<Object> events = List.ofAll(Arrays.asList(eventsArray));
        bh.consume(events);
    }
}
