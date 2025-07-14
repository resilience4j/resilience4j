package io.github.resilience4j.timelimiter.internal;

import io.github.resilience4j.core.ExecutorServiceFactory;
import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.vavr.control.Try;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@RunWith(Parameterized.class)
public class TimeLimiterTest extends ThreadModeTestBase {

    private static final String TIME_LIMITER_NAME = "TestTimeLimiter";
    private static final Duration TIMEOUT = Duration.ofMillis(1000);
    private ScheduledExecutorService scheduler;

    public TimeLimiterTest(ThreadType threadType) {
        super(threadType);
    }

    @Parameterized.Parameters(name = "{0} thread mode")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    @Before
    public void setUp() {
        // Reset the scheduler for each test (ThreadModeTestBase handles thread mode setup)
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @After
    public void tearDown() {
        // Clean up scheduler (ThreadModeTestBase handles thread mode cleanup)
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void shouldReturnCorrectTimeoutDuration() {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);
        assertThat(timeLimiter).isNotNull();
        assertThat(timeLimiter.getTimeLimiterConfig().getTimeoutDuration())
            .isEqualTo(timeoutDuration);
    }

    @Test
    public void shouldThrowTimeoutExceptionAndInvokeCancel() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(timeoutDuration)
            .build();
        TimeLimiter timeLimiter = TimeLimiter.of(TIME_LIMITER_NAME,timeLimiterConfig);

        @SuppressWarnings("unchecked")
        Future<Integer> mockFuture = (Future<Integer>) mock(Future.class);

        Supplier<Future<Integer>> supplier = () -> mockFuture;
        given(mockFuture.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS))
            .willThrow(new TimeoutException());

        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);
        Try<Integer> decoratedResult = Try.ofCallable(decorated);

        assertThat(decoratedResult.isFailure()).isTrue();
        assertThat(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);
        assertThat(decoratedResult.getCause()).hasMessage(TimeLimiter.createdTimeoutExceptionWithName(TIME_LIMITER_NAME, null).getMessage());

        then(mockFuture).should().cancel(true);
    }

    @Test
    public void shouldThrowTimeoutExceptionWithCompletionStage() throws Exception {
        Duration timeoutDuration = Duration.ofMillis(300);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);
        scheduler = ExecutorServiceFactory.newSingleThreadScheduledExecutor("timeout-test-" + threadType);

        Supplier<CompletionStage<Integer>> supplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                // sleep for timeout.
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // nothing
            }
            return 0;
        });

        CompletionStage<Integer> decorated = TimeLimiter
            .decorateCompletionStage(timeLimiter, scheduler, supplier).get();
        Try<Integer> decoratedResult = Try.ofCallable(() -> decorated.toCompletableFuture().get());
        assertThat(decoratedResult.isFailure()).isTrue();
        assertThat(decoratedResult.getCause()).isInstanceOf(ExecutionException.class)
            .hasCauseExactlyInstanceOf(TimeoutException.class);
    }

    @Test
    public void shouldThrowTimeoutExceptionAndNotInvokeCancel() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter
            .of(TimeLimiterConfig.custom().timeoutDuration(timeoutDuration)
                .cancelRunningFuture(false).build());

        @SuppressWarnings("unchecked")
        Future<Integer> mockFuture = (Future<Integer>) mock(Future.class);

        Supplier<Future<Integer>> supplier = () -> mockFuture;
        given(mockFuture.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS))
            .willThrow(new TimeoutException());

        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);
        Try<Integer> decoratedResult = Try.ofCallable(decorated);

        assertThat(decoratedResult.isFailure()).isTrue();
        assertThat(decoratedResult.getCause()).isInstanceOf(TimeoutException.class);

        then(mockFuture).should(never()).cancel(true);
    }

    @Test
    public void shouldReturnResult() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);

        @SuppressWarnings("unchecked")
        Future<Integer> mockFuture = (Future<Integer>) mock(Future.class);

        Supplier<Future<Integer>> supplier = () -> mockFuture;
        given(mockFuture.get(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS)).willReturn(42);

        int result = timeLimiter.executeFutureSupplier(supplier);
        assertThat(result).isEqualTo(42);

        int result2 = timeLimiter.decorateFutureSupplier(supplier).call();
        assertThat(result2).isEqualTo(42);
    }

    @Test
    public void shouldReturnResultWithCompletionStage() throws Exception {
        Duration timeoutDuration = Duration.ofSeconds(1);
        TimeLimiter timeLimiter = TimeLimiter.of(timeoutDuration);
        scheduler = ExecutorServiceFactory.newSingleThreadScheduledExecutor("result-test-" + threadType);

        Supplier<CompletionStage<Integer>> supplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                // sleep but not timeout.
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // nothing
            }
            return 42;
        });

        int result = timeLimiter.executeCompletionStage(scheduler, supplier).toCompletableFuture()
            .get();
        assertThat(result).isEqualTo(42);

        int result2 = timeLimiter.decorateCompletionStage(scheduler, supplier).get()
            .toCompletableFuture().get();
        assertThat(result2).isEqualTo(42);
    }

    @Test
    public void unwrapExecutionException() {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults();
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Supplier<Future<Integer>> supplier = () -> executorService.submit(() -> {
            throw new RuntimeException();
        });
        Callable<Integer> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, supplier);

        Try<Integer> decoratedResult = Try.ofCallable(decorated);

        assertThat(decoratedResult.getCause() instanceof RuntimeException).isTrue();
    }

    @Test
    public void shouldSetGivenName() {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults("TEST");
        assertThat(timeLimiter.getName()).isEqualTo("TEST");
    }

    @Test
    public void shouldUseCorrectThreadTypeForScheduler() throws Exception {
        // Create scheduler via ExecutorServiceFactory which should respect thread mode
        scheduler = ExecutorServiceFactory.newSingleThreadScheduledExecutor("timelimiter-test-" + threadType);
        
        // Create TimeLimiter
        TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
            .timeoutDuration(TIMEOUT)
            .build());
        
        // Setup a task to check if it's running on the expected thread type
        AtomicBoolean ranOnExpectedThreadType = new AtomicBoolean(false);
        
        // Create executor that creates threads matching our expected mode
        ExecutorService taskExecutor = isVirtualThreadMode() ? 
            Executors.newVirtualThreadPerTaskExecutor() : 
            Executors.newSingleThreadExecutor();
            
        try {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate some work
                    Thread.sleep(50);
                    boolean expectedThreadType = isVirtualThreadMode() ? 
                        Thread.currentThread().isVirtual() : 
                        !Thread.currentThread().isVirtual();
                    ranOnExpectedThreadType.set(expectedThreadType);
                    return true;
                } catch (InterruptedException e) {
                    return false;
                }
            }, taskExecutor);
            
            // Decorate with TimeLimiter
            Supplier<CompletionStage<Boolean>> decoratedSupplier = timeLimiter.decorateCompletionStage(
                scheduler, () -> future);
            
            // Execute and get result
            Boolean result = decoratedSupplier.get().toCompletableFuture().get(2, TimeUnit.SECONDS);
            
            // Verify execution was successful
            assertThat(result).isTrue();
            
            // Verify that the timeout handling uses the expected thread type
            CompletableFuture<Boolean> threadTypeFuture = new CompletableFuture<>();
            scheduler.execute(() -> {
                boolean isExpectedType = isVirtualThreadMode() ? 
                    Thread.currentThread().isVirtual() : 
                    !Thread.currentThread().isVirtual();
                threadTypeFuture.complete(isExpectedType);
            });
            
            // Verify the scheduler used the expected thread type
            Boolean usedExpectedThreadType = threadTypeFuture.get(1, TimeUnit.SECONDS);
            assertThat(usedExpectedThreadType)
                .as("TimeLimiter's scheduler should use " + threadType + " threads")
                .isTrue();
                
            // Also verify our test ran on the expected thread type
            assertThat(ranOnExpectedThreadType.get())
                .as("CompletableFuture execution should run on " + threadType + " thread")
                .isTrue();
        } finally {
            taskExecutor.shutdownNow();
        }
    }

    @Test 
    public void shouldTimeoutAndCancelOnCorrectThreadType() throws Exception {
        // Create a CountDownLatch to track if our task was interrupted
        CountDownLatch interruptedLatch = new CountDownLatch(1);
        
        // Create executor service for the test  
        ExecutorService executor = isVirtualThreadMode() ? 
            Executors.newVirtualThreadPerTaskExecutor() : 
            Executors.newSingleThreadExecutor();
            
        try {
            // Create TimeLimiter with short timeout
            TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(50))
                .cancelRunningFuture(true)
                .build());
            
            // Create a blocking callable that will run longer than our timeout
            Callable<String> longRunningTask = () -> {
                try {
                    Thread.sleep(10000); // Sleep for 10 seconds
                    return "Task completed";
                } catch (InterruptedException e) {
                    interruptedLatch.countDown(); // Signal that we were interrupted
                    throw e; // Rethrow to properly handle interruption
                }
            };
            
            // Submit the callable to get a cancellable future
            Future<String> future = executor.submit(longRunningTask);
            
            // Now use the TimeLimiter directly on this Future
            try {
                // This should timeout and cancel the future
                timeLimiter.decorateFutureSupplier(() -> future).call();
                
                // Should not reach here
                fail("Expected timeout exception");
            } catch (TimeoutException e) {
                // Expected - timeout occurred
                
                // Wait for the interruption to propagate
                boolean wasInterrupted = interruptedLatch.await(500, TimeUnit.MILLISECONDS);
                
                // Verify the task was interrupted
                assertThat(wasInterrupted)
                    .as("Task should have been interrupted due to cancellation")
                    .isTrue();
                
                // Also verify the future was cancelled
                assertThat(future.isCancelled())
                    .as("Future should have been cancelled")
                    .isTrue();
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
