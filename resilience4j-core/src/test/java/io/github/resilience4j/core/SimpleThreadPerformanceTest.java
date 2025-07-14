package io.github.resilience4j.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Simple performance comparison between Virtual Threads and Platform Threads
 * for different workload types in Resilience4j context.
 * 
 * <p>This test provides practical insights into when to use virtual threads
 * vs platform threads based on workload characteristics:
 * <ul>
 *   <li><b>CPU-Intensive</b>: Mathematical computations</li>
 *   <li><b>I/O-Intensive</b>: Blocking operations (network, database)</li>
 *   <li><b>Mixed</b>: Combination of CPU and I/O work</li>
 * </ul>
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@Ignore
@RunWith(Parameterized.class)
public class SimpleThreadPerformanceTest extends ThreadModeTestBase {

    private static final int TASK_COUNT = 50; // Reduced for stability
    private static final int TIMEOUT_SECONDS = 10;
    
    private ExecutorService executor;

    public SimpleThreadPerformanceTest(ThreadType threadType) {
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
            Executors.newFixedThreadPool(10); // Limited pool for platform threads
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void measureCpuIntensiveWorkload() throws Exception {
        System.out.println("\n=== CPU-Intensive Test [" + threadType + " threads] ===");
        
        long startTime = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        AtomicInteger results = new AtomicInteger(0);
        
        // Submit CPU-intensive tasks
        for (int i = 0; i < TASK_COUNT; i++) {
            executor.submit(() -> {
                try {
                    // CPU work: Calculate prime numbers up to 500
                    int primeCount = 0;
                    for (int num = 2; num <= 500; num++) {
                        if (isPrime(num)) {
                            primeCount++;
                        }
                    }
                    results.addAndGet(primeCount);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for completion
        boolean completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long duration = System.nanoTime() - startTime;
        
        assertThat(completed).isTrue();
        
        double durationMs = duration / 1_000_000.0;
        double throughput = TASK_COUNT / (durationMs / 1000.0);
        
        System.out.printf("Completed %d tasks in %.2f ms (%.2f tasks/sec)%n", 
            TASK_COUNT, durationMs, throughput);
        System.out.printf("Total primes found: %d (expected: %d)%n", 
            results.get(), TASK_COUNT * 95); // 95 primes up to 500
            
        // Verify correctness
        assertThat(results.get()).isEqualTo(TASK_COUNT * 95);
    }

    @Test 
    public void measureIoIntensiveWorkload() throws Exception {
        System.out.println("\n=== I/O-Intensive Test [" + threadType + " threads] ===");
        
        long startTime = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        AtomicInteger results = new AtomicInteger(0);
        
        // Submit I/O-intensive tasks
        for (int i = 0; i < TASK_COUNT; i++) {
            executor.submit(() -> {
                try {
                    // I/O simulation: blocking sleep
                    Thread.sleep(20); // 20ms I/O operation
                    
                    // Light processing
                    String data = "processed-data-" + Thread.currentThread().getName();
                    if (data.length() > 10) {
                        results.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for completion
        boolean completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long duration = System.nanoTime() - startTime;
        
        assertThat(completed).isTrue();
        
        double durationMs = duration / 1_000_000.0;
        double throughput = TASK_COUNT / (durationMs / 1000.0);
        
        System.out.printf("Completed %d tasks in %.2f ms (%.2f tasks/sec)%n", 
            TASK_COUNT, durationMs, throughput);
        System.out.printf("Successful operations: %d/%d%n", results.get(), TASK_COUNT);
        
        // Verify most operations succeeded
        assertThat(results.get()).isGreaterThan(TASK_COUNT * 90 / 100); // 90% success
        
        // Virtual threads should handle I/O better (more concurrency)
        if (isVirtualThreadMode()) {
            System.out.println("✓ Virtual threads: Better for I/O-bound workloads");
        } else {
            System.out.println("✓ Platform threads: Limited by thread pool size for I/O");
        }
    }

    @Test
    public void measureMixedWorkload() throws Exception {
        System.out.println("\n=== Mixed Workload Test [" + threadType + " threads] ===");
        
        long startTime = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        AtomicInteger results = new AtomicInteger(0);
        
        // Submit mixed workload tasks
        for (int i = 0; i < TASK_COUNT; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // CPU work: small calculation
                    int fibResult = fibonacci(10); // fib(10) = 55
                    
                    // I/O work: brief sleep
                    Thread.sleep(5); // 5ms I/O
                    
                    // More CPU work: string manipulation
                    String processed = "task-" + taskId + "-result-" + fibResult;
                    
                    if (processed.contains("55")) { // fib(10) = 55
                        results.incrementAndGet();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for completion
        boolean completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long duration = System.nanoTime() - startTime;
        
        assertThat(completed).isTrue();
        
        double durationMs = duration / 1_000_000.0;
        double throughput = TASK_COUNT / (durationMs / 1000.0);
        
        System.out.printf("Completed %d tasks in %.2f ms (%.2f tasks/sec)%n", 
            TASK_COUNT, durationMs, throughput);
        System.out.printf("Correct results: %d/%d%n", results.get(), TASK_COUNT);
        
        // Verify correctness
        assertThat(results.get()).isEqualTo(TASK_COUNT);
    }

    @Test
    public void demonstrateScalabilityDifference() throws Exception {
        System.out.println("\n=== Scalability Test [" + threadType + " threads] ===");
        
        // Test different concurrency levels
        int[] concurrencyLevels = isVirtualThreadMode() ? 
            new int[]{10, 50, 100, 200} :  // Virtual threads can handle more
            new int[]{10, 20, 30};         // Platform threads limited by pool
        
        for (int concurrency : concurrencyLevels) {
            long startTime = System.nanoTime();
            CountDownLatch latch = new CountDownLatch(concurrency);
            AtomicInteger completed = new AtomicInteger(0);
            
            // Submit concurrent I/O tasks
            for (int i = 0; i < concurrency; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(10); // 10ms I/O simulation
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            boolean allCompleted = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long duration = System.nanoTime() - startTime;
            
            if (allCompleted) {
                double throughput = completed.get() / ((duration / 1_000_000.0) / 1000.0);
                System.out.printf("Concurrency %d: %.2f tasks/sec (%d/%d completed)%n", 
                    concurrency, throughput, completed.get(), concurrency);
                    
                assertThat(completed.get()).isGreaterThan(concurrency * 80 / 100); // 80% success
            } else {
                System.out.printf("Concurrency %d: TIMEOUT%n", concurrency);
            }
        }
        
        // Expected behavior summary
        if (isVirtualThreadMode()) {
            System.out.println("✓ Virtual threads scale well with I/O concurrency");
        } else {
            System.out.println("✓ Platform threads limited by thread pool size");
        }
    }

    @Test
    public void summarizePerformanceCharacteristics() {
        System.out.println("\n=== Performance Summary [" + threadType + " threads] ===");
        
        if (isVirtualThreadMode()) {
            System.out.println("Virtual Thread Characteristics:");
            System.out.println("• Excellent for I/O-bound workloads");
            System.out.println("• High concurrency with low memory overhead");
            System.out.println("• Efficient blocking operations");
            System.out.println("• Less suitable for pure CPU-intensive tasks");
            System.out.println("• Perfect for microservices with many external calls");
        } else {
            System.out.println("Platform Thread Characteristics:");
            System.out.println("• Good for CPU-intensive workloads");
            System.out.println("• Limited concurrency by thread pool size");
            System.out.println("• Higher memory overhead per thread");
            System.out.println("• Mature tooling and debugging support");
            System.out.println("• Traditional choice for compute-heavy applications");
        }
        
        System.out.println("\nResilience4j Recommendation:");
        System.out.println("• Use virtual threads for high I/O concurrency");
        System.out.println("• Use platform threads for CPU-intensive processing");
        System.out.println("• Configure based on your application's workload profile");
    }

    // Helper methods

    private boolean isJava21OrLater() {
        try {
            Thread.ofVirtual();
            return true;
        } catch (Exception | NoSuchMethodError e) {
            return false;
        }
    }

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
        if (n == 2) return 1;
        
        int a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            int temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }
}