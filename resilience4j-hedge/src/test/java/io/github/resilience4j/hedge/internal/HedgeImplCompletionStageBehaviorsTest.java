/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.ContextPropagator;
import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.HedgeConfig;
import io.github.resilience4j.hedge.event.HedgeEvent;
import io.github.resilience4j.test.TestContextPropagators;
import org.junit.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;


public class HedgeImplCompletionStageBehaviorsTest {

    private static final Duration FIFTY_MILLIS = Duration.ofMillis(50);
    private static final String PRIMARY = "Primary";
    private static final String HEDGED = "Hedged";
    private static final int FAST_SPEED = 10;
    private static final int SLOW_SPEED = 200;
    private static final String PRIMARY_EXCEPTION_MESSAGE = "Primary threw an exception";
    private static final String HEDGE_EXCEPTION_MESSAGE = "Hedge threw an exception";
    private static final RuntimeException HEDGE_RUNTIME_EXCEPTION = new RuntimeException(HEDGE_EXCEPTION_MESSAGE);
    private static final RuntimeException PRIMARY_RUNTIME_EXCEPTION = new RuntimeException(PRIMARY_EXCEPTION_MESSAGE);
    private static final HedgeBehaviorSpecification FAST_PRIMARY_SUCCESS = new HedgeBehaviorSpecification(FAST_SPEED, PRIMARY, null);
    private static final HedgeBehaviorSpecification FAST_PRIMARY_FAILURE = new HedgeBehaviorSpecification(FAST_SPEED, PRIMARY, PRIMARY_RUNTIME_EXCEPTION);
    private static final HedgeBehaviorSpecification FAST_HEDGE_SUCCESS = new HedgeBehaviorSpecification(FAST_SPEED, HEDGED, null);
    private static final HedgeBehaviorSpecification FAST_HEDGE_FAILURE = new HedgeBehaviorSpecification(FAST_SPEED, HEDGED, HEDGE_RUNTIME_EXCEPTION);
    private static final HedgeBehaviorSpecification SLOW_PRIMARY_SUCCESS = new HedgeBehaviorSpecification(SLOW_SPEED, PRIMARY, null);
    private static final HedgeBehaviorSpecification SLOW_PRIMARY_FAILURE = new HedgeBehaviorSpecification(SLOW_SPEED, PRIMARY, PRIMARY_RUNTIME_EXCEPTION);
    private static final HedgeBehaviorSpecification SLOW_HEDGE_SUCCESS = new HedgeBehaviorSpecification(SLOW_SPEED, HEDGED, null);
    private static final HedgeBehaviorSpecification SLOW_HEDGE_FAILURE = new HedgeBehaviorSpecification(SLOW_SPEED, HEDGED, HEDGE_RUNTIME_EXCEPTION);
    private final Logger logger = mock(Logger.class);
    private static final ThreadLocal<String> threadLocal = ThreadLocal.withInitial(() -> "UNINITIALIZED");

    @Test
    public void shouldUsePropagatorsCorrectly() {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        ContextPropagator propagator = new TestContextPropagators.TestThreadLocalContextPropagator(threadLocal);
        HedgeConfig config = HedgeConfig
            .custom()
            .preconfiguredDuration(FIFTY_MILLIS)
            .withContextPropagators(propagator)
            .build();
        threadLocal.set("LOCAL CONTEXT IS NOW SET");
        //   If the standard ScheduledThreadPoolExecutor is used to execute the code,
        //   the propagators don't have any context to propagate because it is already
        //   lost from the driver thread to the primary executor. Switch the executors in the next two lines.
        //        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);
        ScheduledThreadPoolExecutor executor = ContextAwareScheduledThreadPoolExecutor.newScheduledThreadPool().contextPropagators(propagator).corePoolSize(10).build();
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications, executor);
        Hedge hedge = Hedge.of(config);
        hedge.getEventPublisher().onSecondarySuccess(
            event -> logger.info(event.getEventType().toString())
        );
        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();
    }


    @Test
    public void shouldReturnHedgeSuccessOnSlowPrimarySuccessFastHedgeSuccess() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onSecondarySuccess(
            event -> logger.info(event.getEventType().toString())
        );
        hedge.getEventPublisher().onSecondaryFailure(event -> logger.info(event.getEventType().toString()));
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));

        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();
        assertThat(hedged.get()).isEqualTo(HEDGED);
        then(logger).should().info(HedgeEvent.Type.SECONDARY_SUCCESS.toString());

        assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
        assertThat(hedge.getMetrics().getPrimaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getPrimarySuccessCount()).isZero();
        assertThat(hedge.getMetrics().getSecondaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getSecondarySuccessCount()).isEqualTo(1);
        assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
    }

    @Test
    public void slowPrimarySuccessFastHedgeFailureReturnsHedgeFailure() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_FAILURE};
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onSecondaryFailure(event -> logger.info(event.getEventType().toString()));
        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();

        try {
            String result = hedged.get();
            fail("call should not succeed. instead returned " + result);
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(HEDGE_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.SECONDARY_FAILURE.toString());
            assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
            assertThat(hedge.getMetrics().getPrimaryFailureCount()).isZero();
            assertThat(hedge.getMetrics().getPrimarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryFailureCount()).isEqualTo(1);
            assertThat(hedge.getMetrics().getSecondarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
        }
    }

    @Test
    public void slowPrimaryFailureFastHedgeSuccessReturnsHedgeSuccess() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onSecondarySuccess(event -> logger.info(event.getEventType().toString()));
        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();

        assertThat(hedged.get()).isEqualTo(HEDGED);
        then(logger).should().info(HedgeEvent.Type.SECONDARY_SUCCESS.toString());
        assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
        assertThat(hedge.getMetrics().getPrimaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getPrimarySuccessCount()).isZero();
        assertThat(hedge.getMetrics().getSecondaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getSecondarySuccessCount()).isEqualTo(1);
        assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
    }

    @Test
    public void slowPrimaryFailureFastHedgeFailureReturnsHedgeFailure() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_FAILURE, FAST_HEDGE_FAILURE};
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onSecondaryFailure(event -> logger.info(event.getEventType().toString()));
        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();

        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(HEDGE_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.SECONDARY_FAILURE.toString());
            assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
            assertThat(hedge.getMetrics().getPrimaryFailureCount()).isZero();
            assertThat(hedge.getMetrics().getPrimarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryFailureCount()).isEqualTo(1);
            assertThat(hedge.getMetrics().getSecondarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
        }
    }

    @Test
    public void slowPrimarySuccessSlowHedgeSuccessReturnsPrimarySuccess() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, SLOW_HEDGE_SUCCESS};
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();

        assertThat(hedged.get()).isEqualTo(PRIMARY);
        then(logger).should().info(HedgeEvent.Type.PRIMARY_SUCCESS.toString());
        assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
        assertThat(hedge.getMetrics().getPrimaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getPrimarySuccessCount()).isEqualTo(1);
        assertThat(hedge.getMetrics().getSecondaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getSecondarySuccessCount()).isZero();
        assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
    }

    @Test
    public void slowPrimarySuccessSlowHedgeFailureReturnsPrimarySuccess() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, SLOW_HEDGE_FAILURE};
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();

        assertThat(hedged.get()).isEqualTo(PRIMARY);
        then(logger).should().info(HedgeEvent.Type.PRIMARY_SUCCESS.toString());
        assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
        assertThat(hedge.getMetrics().getPrimaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getPrimarySuccessCount()).isEqualTo(1);
        assertThat(hedge.getMetrics().getSecondaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getSecondarySuccessCount()).isZero();
        assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
    }

    @Test
    public void slowPrimaryFailureSlowHedgeSuccessReturnsPrimaryFailure() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_FAILURE, SLOW_HEDGE_SUCCESS};
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));
        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();

        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(PRIMARY_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.PRIMARY_FAILURE.toString());
            assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
            assertThat(hedge.getMetrics().getPrimaryFailureCount()).isEqualTo(1);
            assertThat(hedge.getMetrics().getPrimarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryFailureCount()).isZero();
            assertThat(hedge.getMetrics().getSecondarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
        }
    }

    @Test
    public void slowPrimaryFailureSlowHedgeFailureReturnsPrimaryFailure() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_FAILURE, SLOW_HEDGE_FAILURE};
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));
        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();

        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(PRIMARY_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.PRIMARY_FAILURE.toString());
            assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
            assertThat(hedge.getMetrics().getPrimaryFailureCount()).isEqualTo(1);
            assertThat(hedge.getMetrics().getPrimarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryFailureCount()).isZero();
            assertThat(hedge.getMetrics().getSecondarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
        }
    }

    @Test
    public void fastPrimaryFailureDoesNotHedge() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {FAST_PRIMARY_FAILURE, FAST_HEDGE_SUCCESS};
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));
        CompletableFuture<String> hedged = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();

        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getMessage()).isEqualTo(PRIMARY_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.PRIMARY_FAILURE.toString());
            assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
            assertThat(hedge.getMetrics().getPrimaryFailureCount()).isEqualTo(1);
            assertThat(hedge.getMetrics().getPrimarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryFailureCount()).isZero();
            assertThat(hedge.getMetrics().getSecondarySuccessCount()).isZero();
            assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
        }
    }

    @Test
    public void fastPrimarySuccessDoesNotHedge() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {FAST_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Hedge hedge = Hedge.of(FIFTY_MILLIS);
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        Supplier<CompletableFuture<String>> futureSupplier = hedgingCompletionStage(hedgeBehaviorSpecifications);

        CompletableFuture<String> completableFuture = (CompletableFuture<String>) hedge.decorateCompletionStage(futureSupplier).get();

        assertThat(completableFuture.get()).isEqualTo(PRIMARY);
        then(logger).should().info(HedgeEvent.Type.PRIMARY_SUCCESS.toString());
        assertThat(hedge.getMetrics().getCurrentHedgeDelay().toMillis()).isLessThan(SLOW_SPEED);
        assertThat(hedge.getMetrics().getPrimaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getPrimarySuccessCount()).isEqualTo(1);
        assertThat(hedge.getMetrics().getSecondaryFailureCount()).isZero();
        assertThat(hedge.getMetrics().getSecondarySuccessCount()).isZero();
        assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isZero();
    }

    private Supplier<CompletableFuture<String>> hedgingCompletionStage(HedgeBehaviorSpecification[] hedgeBehaviorSpecifications) {
        AtomicInteger iterations = new AtomicInteger(0);
        return () -> CompletableFuture.supplyAsync(
            () -> {
                int iteration = iterations.getAndIncrement();
                try {
                    Thread.sleep(hedgeBehaviorSpecifications[iteration].sleep);
                } catch (InterruptedException e) {
                    //do nothing
                }
                if (hedgeBehaviorSpecifications[iteration].exception != null) {
                    throw hedgeBehaviorSpecifications[iteration].exception;
                } else {
                    return hedgeBehaviorSpecifications[iteration].result;
                }
            }
        );
    }

    /**
     * Creates a supplier of defined behaviors. The first invocation of the supplier will return the non-hedged behavior.
     * The second invocation will return the hedged behavior. This includes errors, return values and execution time.
     *
     * @param hedgeBehaviorSpecifications
     * @param executor
     * @return the supplier of the specified behaviors
     */
    private Supplier<CompletableFuture<String>> hedgingCompletionStage(HedgeBehaviorSpecification[] hedgeBehaviorSpecifications, ScheduledThreadPoolExecutor executor) {
        AtomicInteger iterations = new AtomicInteger(0);
        return () -> CompletableFuture.supplyAsync(
            () -> {
                int iteration = iterations.getAndIncrement();
                try {
                    //here you can access threadLocal's new value IF the primary executor passed it along.
                    //System.out.println("runnable thinks the threadlocal is '" + threadLocal.get() + "' for the " + hedgeBehaviorSpecifications[iteration].result);
                    Thread.sleep(hedgeBehaviorSpecifications[iteration].sleep);
                } catch (InterruptedException e) {
                    //do nothing
                }
                if (hedgeBehaviorSpecifications[iteration].exception != null) {
                    throw hedgeBehaviorSpecifications[iteration].exception;
                } else {
                    return hedgeBehaviorSpecifications[iteration].result;
                }
            },
            executor
        );
    }

    private static class HedgeBehaviorSpecification {
        final int sleep;
        final String result;
        final RuntimeException exception;

        public HedgeBehaviorSpecification(int sleep, String result, RuntimeException exception) {
            this.sleep = sleep;
            this.result = result;
            this.exception = exception;
        }
    }

}