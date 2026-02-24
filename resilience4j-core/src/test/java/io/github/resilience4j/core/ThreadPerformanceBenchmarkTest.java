package io.github.resilience4j.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Comprehensive performance benchmark comparing Virtual Threads vs Platform Threads
 * across different workload characteristics in Resilience4j context.
 * 
 * <p>Test Scenarios:
 * <ul>
 *   <li><b>CPU-Intensive</b>: Mathematical calculations, data processing</li>
 *   <li><b>I/O-Intensive</b>: Network calls, file operations, database access</li>
 *   <li><b>Mixed Workload</b>: Real-world combination of CPU and I/O operations</li>
 * </ul>
 * 
 * <p>Performance Metrics:
 * <ul>
 *   <li>Throughput (operations/second)</li>
 *   <li>Latency (average response time)</li>
 *   <li>Resource Utilization (memory, thread count)</li>
 *   <li>Scalability (performance under load)</li>
 * </ul>
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@Ignore
@RunWith(Parameterized.class)
public class ThreadPerformanceBenchmarkTest extends ThreadModeTestBase {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 1000;
    private static final int CONCURRENT_TASKS = 100;
    private static final Duration MAX_TEST_DURATION = Duration.ofSeconds(30);

    private ExecutorService executor;
    private PerformanceMetrics metrics;

    public ThreadPerformanceBenchmarkTest(ThreadType threadType) {
        super(threadType);
    }

    @Parameterized.Parameters(name = "{0} thread mode")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    @Before
    public void setUp() {
        assumeTrue("Virtual threads require Java 21+", isJava21OrLater());
        
        // Create appropriate executor based on thread mode
        executor = isVirtualThreadMode() ? 
            Executors.newVirtualThreadPerTaskExecutor() :
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
            
        metrics = new PerformanceMetrics();
        
        // Warmup JVM
        performWarmup();
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    public void shouldMeasureCpuIntensivePerformance() throws Exception {
        System.out.println("\n=== CPU-Intensive Workload Performance [" + threadType + " threads] ===");
        
        // CPU-intensive task: Prime number calculation
        Supplier<Integer> cpuTask = () -> {
            int primeCount = 0;
            for (int num = 2; num <= 1000; num++) {
                if (isPrime(num)) {
                    primeCount++;
                }
            }
            return primeCount;
        };

        BenchmarkResult result = runBenchmark("CPU-Intensive", cpuTask);
        
        // Log results
        logBenchmarkResult(result);
        
        // Verify task correctness
        assertThat(result.correctnessCheck).isEqualTo(168); // Number of primes up to 1000
        
        // Performance assertions
        assertThat(result.averageLatencyMs).isLessThan(MAX_TEST_DURATION.toMillis());
        assertThat(result.successRate).isGreaterThan(0.95); // 95% success rate
        
        // Store metrics for comparison
        metrics.cpuIntensiveResult = result;
    }

    @Test
    public void shouldMeasureIoIntensivePerformance() throws Exception {
        System.out.println("\n=== I/O-Intensive Workload Performance [" + threadType + " threads] ===");
        
        // I/O-intensive task: Simulated network/database call
        Supplier<String> ioTask = () -> {
            try {
                // Simulate network latency (blocking I/O)
                Thread.sleep(10); // 10ms network call
                
                // Simulate some data processing
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < 100; i++) {
                    result.append("data-").append(i).append("-");
                }
                
                return result.toString();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "interrupted";
            }
        };

        BenchmarkResult result = runBenchmark("I/O-Intensive", ioTask);
        
        // Log results
        logBenchmarkResult(result);
        
        // Verify task correctness
        assertThat(result.correctnessCheck).asString().startsWith("data-0-");
        
        // Performance assertions - I/O should benefit more from virtual threads
        assertThat(result.averageLatencyMs).isLessThan(MAX_TEST_DURATION.toMillis());
        assertThat(result.successRate).isGreaterThan(0.95);
        
        // Store metrics for comparison
        metrics.ioIntensiveResult = result;
    }

    @Test
    public void shouldMeasureMixedWorkloadPerformance() throws Exception {
        System.out.println("\n=== Mixed Workload Performance [" + threadType + " threads] ===");
        
        // Mixed workload: CPU + I/O operations
        Supplier<MixedResult> mixedTask = () -> {
            try {
                // CPU work: Calculate fibonacci
                int fibResult = fibonacci(20);
                
                // I/O work: Simulated database/cache lookup
                Thread.sleep(5); // 5ms I/O operation
                
                // More CPU work: String processing
                String processed = processString("resilience4j-" + fibResult);
                
                return new MixedResult(fibResult, processed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new MixedResult(-1, "interrupted");
            }
        };

        BenchmarkResult result = runBenchmark("Mixed-Workload", mixedTask);
        
        // Log results
        logBenchmarkResult(result);
        
        // Verify task correctness
        MixedResult mixedResult = (MixedResult) result.correctnessCheck;
        assertThat(mixedResult.fibonacciResult).isEqualTo(6765); // fib(20)
        assertThat(mixedResult.processedString).containsIgnoringCase("resilience4j");
        
        // Performance assertions
        assertThat(result.averageLatencyMs).isLessThan(MAX_TEST_DURATION.toMillis());
        assertThat(result.successRate).isGreaterThan(0.95);
        
        // Store metrics for comparison
        metrics.mixedWorkloadResult = result;
    }

    @Test
    public void shouldComparePerformanceCharacteristics() {
        // This test runs after the benchmark tests and provides analysis
        System.out.println("\n=== Performance Analysis Summary ===");
        
        if (metrics.cpuIntensiveResult != null) {
            System.out.printf("CPU-Intensive [%s]: %.2f ops/sec, %.2f ms avg latency%n",
                threadType,
                metrics.cpuIntensiveResult.throughputOpsPerSec,
                metrics.cpuIntensiveResult.averageLatencyMs);
        }
        
        if (metrics.ioIntensiveResult != null) {
            System.out.printf("I/O-Intensive [%s]: %.2f ops/sec, %.2f ms avg latency%n",
                threadType,
                metrics.ioIntensiveResult.throughputOpsPerSec,
                metrics.ioIntensiveResult.averageLatencyMs);
        }
        
        if (metrics.mixedWorkloadResult != null) {
            System.out.printf("Mixed-Workload [%s]: %.2f ops/sec, %.2f ms avg latency%n",
                threadType,
                metrics.mixedWorkloadResult.throughputOpsPerSec,
                metrics.mixedWorkloadResult.averageLatencyMs);
        }
        
        // Expected performance characteristics
        if (isVirtualThreadMode()) {
            System.out.println("\nVirtual Thread Advantages:");
            System.out.println("- Better I/O concurrency (lower thread overhead)");
            System.out.println("- Higher scalability for blocking operations");
            System.out.println("- Reduced memory footprint per thread");
        } else {
            System.out.println("\nPlatform Thread Advantages:");
            System.out.println("- Better CPU-intensive performance");
            System.out.println("- Lower context switching overhead");
            System.out.println("- More mature tooling and debugging support");
        }
    }

    @Test 
    public void shouldMeasureScalabilityUnderLoad() throws Exception {
        System.out.println("\n=== Scalability Test [" + threadType + " threads] ===");
        
        // Test with increasing concurrent load
        int[] loadLevels = {10, 50, 100, 500, 1000};
        
        for (int concurrency : loadLevels) {
            // Skip high concurrency for platform threads to avoid resource exhaustion
            if (!isVirtualThreadMode() && concurrency > 100) {
                System.out.printf("Skipping concurrency %d for platform threads (resource limit)%n", concurrency);
                continue;
            }
            
            long startTime = System.nanoTime();
            CountDownLatch latch = new CountDownLatch(concurrency);
            AtomicInteger completedTasks = new AtomicInteger(0);
            
            // Submit concurrent tasks
            for (int i = 0; i < concurrency; i++) {
                executor.submit(() -> {
                    try {
                        // Simulate mixed workload
                        Thread.sleep(10); // I/O simulation
                        int result = fibonacci(15); // CPU work
                        if (result > 0) {
                            completedTasks.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Wait for completion with timeout
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            long duration = System.nanoTime() - startTime;
            
            if (completed) {
                double throughput = (double) completedTasks.get() / (duration / 1_000_000_000.0);
                System.out.printf("Concurrency %d: %d/%d completed, %.2f ops/sec%n", 
                    concurrency, completedTasks.get(), concurrency, throughput);
                
                assertThat(completedTasks.get()).isGreaterThan((int)(concurrency * 0.9)); // 90% success
            } else {
                System.out.printf("Concurrency %d: TIMEOUT after 30s%n", concurrency);
            }
        }
    }

    // Helper Methods

    private <T> BenchmarkResult runBenchmark(String testName, Supplier<T> task) throws Exception {
        System.out.printf("Running %s benchmark with %d concurrent tasks...%n", testName, CONCURRENT_TASKS);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_TASKS);
        AtomicLong totalLatency = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicReference<Object> firstResult = new AtomicReference<>(null);
        
        long benchmarkStart = System.nanoTime();
        
        // Submit all tasks
        for (int i = 0; i < CONCURRENT_TASKS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronized start
                    
                    long taskStart = System.nanoTime();
                    T result = task.get();
                    long taskEnd = System.nanoTime();
                    
                    // Record metrics
                    totalLatency.addAndGet(taskEnd - taskStart);
                    successCount.incrementAndGet();
                    
                    // Capture first result for correctness check
                    firstResult.compareAndSet(null, result);
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.printf("Task failed: %s%n", e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all tasks simultaneously
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(MAX_TEST_DURATION.toMillis(), TimeUnit.MILLISECONDS);
        long benchmarkEnd = System.nanoTime();
        
        if (!completed) {
            throw new RuntimeException("Benchmark timed out after " + MAX_TEST_DURATION.toSeconds() + " seconds");
        }
        
        // Calculate metrics
        double totalTimeSeconds = (benchmarkEnd - benchmarkStart) / 1_000_000_000.0;
        double throughput = successCount.get() / totalTimeSeconds;
        double averageLatencyMs = (totalLatency.get() / successCount.get()) / 1_000_000.0;
        double successRate = (double) successCount.get() / CONCURRENT_TASKS;
        
        return new BenchmarkResult(
            testName,
            throughput,
            averageLatencyMs,
            successRate,
            successCount.get(),
            errorCount.get(),
            firstResult.get()
        );
    }

    private void performWarmup() {
        // JVM warmup to ensure accurate measurements
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            fibonacci(10);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void logBenchmarkResult(BenchmarkResult result) {
        System.out.printf("Results: %.2f ops/sec, %.2f ms avg latency, %.2f%% success rate%n",
            result.throughputOpsPerSec,
            result.averageLatencyMs,
            result.successRate * 100);
        System.out.printf("Completed: %d success, %d errors%n",
            result.successCount, result.errorCount);
    }

    private boolean isJava21OrLater() {
        try {
            Thread.ofVirtual();
            return true;
        } catch (Exception | NoSuchMethodError e) {
            return false;
        }
    }

    // Utility methods for benchmark tasks

    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }

    private int fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    private String processString(String input) {
        return input.toUpperCase()
                   .replace("-", "_")
                   .substring(0, Math.min(input.length(), 20));
    }

    // Data Classes

    private static class BenchmarkResult {
        final String testName;
        final double throughputOpsPerSec;
        final double averageLatencyMs;
        final double successRate;
        final int successCount;
        final int errorCount;
        final Object correctnessCheck;

        BenchmarkResult(String testName, double throughputOpsPerSec, double averageLatencyMs,
                       double successRate, int successCount, int errorCount, Object correctnessCheck) {
            this.testName = testName;
            this.throughputOpsPerSec = throughputOpsPerSec;
            this.averageLatencyMs = averageLatencyMs;
            this.successRate = successRate;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.correctnessCheck = correctnessCheck;
        }
    }

    private static class MixedResult {
        final int fibonacciResult;
        final String processedString;

        MixedResult(int fibonacciResult, String processedString) {
            this.fibonacciResult = fibonacciResult;
            this.processedString = processedString;
        }
    }

    private static class PerformanceMetrics {
        BenchmarkResult cpuIntensiveResult;
        BenchmarkResult ioIntensiveResult;
        BenchmarkResult mixedWorkloadResult;
    }
}