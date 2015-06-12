package io.github.robwin.failsafe;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerRegistry;
import io.github.robwin.retry.Retry;
import javaslang.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;
import static org.assertj.core.api.Assertions.assertThat;

public class FailSafeTest {
    private CircuitBreaker circuitBreaker;
    private Retry retryContext;
    private Timer timer;

    @Before
    public void setUp(){
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("uniqueName");
        retryContext = Retry.ofDefaults();
        MetricRegistry metricRegistry = new MetricRegistry();
        timer = metricRegistry.timer(name("test"));
    }

    @Test
    public void shouldCreateAFailSafeSupplier() {
        Supplier<String> supplier = () -> "Hello World";
        Supplier<String> decoratedSupplier = FailSafe.ofSupplier(supplier)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retryContext)
                .withMetrics(timer)
                .decorate();

        assertThat(decoratedSupplier).isNotNull();
    }

    @Test
    public void shouldCreateAFailSafeFunction() {
        Function<String, String> function = (name) -> "Hello World " + name;
        Function<String, String> decoratedFunction = FailSafe.ofFuction(function)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retryContext)
                .withMetrics(timer)
                .decorate();

        assertThat(decoratedFunction).isNotNull();
    }

    @Test
    public void shouldCreateAFailSafeRunnable() {
        Runnable runnable = () -> System.out.println("Hello world");
        Runnable decoratedRunnable = FailSafe.ofRunnable(runnable)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retryContext)
                .withMetrics(timer)
                .decorate();

        assertThat(decoratedRunnable).isNotNull();
    }

    @Test
    public void shouldCreateAFailSafeCheckedSupplier() {
        Try.CheckedSupplier<String> supplier = () -> "Hello World";
        Try.CheckedSupplier<String> decoratedSupplier = FailSafe.ofCheckedSupplier(supplier)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retryContext)
                .withMetrics(timer)
                .decorate();

        assertThat(decoratedSupplier).isNotNull();
    }

    @Test
    public void shouldCreateAFailSafeCheckedFunction() {
        Try.CheckedFunction<String, String> function = (name) -> "Hello World " + name;
        Try.CheckedFunction<String, String> decoratedFunction = FailSafe.ofCheckedFuction(function)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retryContext)
                .withMetrics(timer)
                .decorate();

        assertThat(decoratedFunction).isNotNull();
    }

    @Test
    public void shouldCreateAFailSafeCheckedRunnable() {
        Try.CheckedRunnable runnable = () -> System.out.println("Hello world");
        Try.CheckedRunnable decoratedRunnable = FailSafe.ofCheckedRunnable(runnable)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retryContext)
                .withMetrics(timer)
                .decorate();

        assertThat(decoratedRunnable).isNotNull();
    }
}
