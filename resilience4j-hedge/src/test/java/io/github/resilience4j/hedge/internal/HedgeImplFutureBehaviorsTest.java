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

import io.github.resilience4j.hedge.Hedge;
import io.github.resilience4j.hedge.event.HedgeEvent;
import org.junit.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;


public class HedgeImplFutureBehaviorsTest {

    private static final Duration HEDGE_ACTIVATION_TIME = Duration.ofMillis(50);
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
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    @Test
    public void shouldReturnValueWhenSuppliedExecutor() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onSecondarySuccess(event -> logger.info(event.getEventType().toString()));

        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);

        assertThat(hedged.get()).isEqualTo(HEDGED);
        then(logger).should().info(HedgeEvent.Type.SECONDARY_SUCCESS.toString());
    }

    @Test
    public void slowPrimarySuccessFastHedgeSuccessReturnsHedgeSuccess() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onSecondarySuccess(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);

        assertThat(hedged.get()).isEqualTo(HEDGED);
        then(logger).should().info(HedgeEvent.Type.SECONDARY_SUCCESS.toString());
    }

    @Test
    public void slowPrimarySuccessFastHedgeFailureReturnsHedgeFailure() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_FAILURE};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onSecondaryFailure(event -> logger.info(event.getEventType().toString()));
        System.out.println(System.currentTimeMillis());
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);
        try {
            String result = hedged.get();
            fail("call should not succeed. instead returned " + result);
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(HEDGE_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.SECONDARY_FAILURE.toString());
        }
    }

    @Test
    public void slowPrimaryFailureFastHedgeSuccessReturnsHedgeSuccess() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onSecondarySuccess(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);
        assertThat(hedged.get()).isEqualTo(HEDGED);
        then(logger).should().info(HedgeEvent.Type.SECONDARY_SUCCESS.toString());
    }

    @Test
    public void slowPrimaryFailureFastHedgeFailureReturnsHedgeFailure() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_FAILURE, FAST_HEDGE_FAILURE};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onSecondaryFailure(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);
        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(HEDGE_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.SECONDARY_FAILURE.toString());
        }
    }

    @Test
    public void slowPrimarySuccessSlowHedgeSuccessReturnsPrimarySuccess() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, SLOW_HEDGE_SUCCESS};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);
        assertThat(hedged.get()).isEqualTo(PRIMARY);
        then(logger).should().info(HedgeEvent.Type.PRIMARY_SUCCESS.toString());
    }

    @Test
    public void slowPrimarySuccessSlowHedgeFailureReturnsPrimarySuccess() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_SUCCESS, SLOW_HEDGE_FAILURE};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);
        assertThat(hedged.get()).isEqualTo(PRIMARY);
        then(logger).should().info(HedgeEvent.Type.PRIMARY_SUCCESS.toString());
    }

    @Test
    public void slowPrimaryFailureSlowHedgeSuccessReturnsPrimaryFailure() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_FAILURE, SLOW_HEDGE_SUCCESS};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);
        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(PRIMARY_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.PRIMARY_FAILURE.toString());
        }
    }

    @Test
    public void slowPrimaryFailureSlowHedgeFailureReturnsPrimaryFailure() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {SLOW_PRIMARY_FAILURE, SLOW_HEDGE_FAILURE};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);
        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(PRIMARY_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.PRIMARY_FAILURE.toString());
        }
    }

    @Test
    public void fastPrimaryFailureDoesNotHedge() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {FAST_PRIMARY_FAILURE, FAST_HEDGE_SUCCESS};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onPrimaryFailure(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);
        try {
            hedged.get();
            fail("call should not succeed");
        } catch (ExecutionException e) {
            assertThat(e.getCause().getCause().getMessage()).isEqualTo(PRIMARY_EXCEPTION_MESSAGE);
            then(logger).should().info(HedgeEvent.Type.PRIMARY_FAILURE.toString());
        }
    }

    @Test
    public void fastPrimarySuccessDoesNotHedge() throws Exception {
        HedgeBehaviorSpecification[] hedgeBehaviorSpecifications = {FAST_PRIMARY_SUCCESS, FAST_HEDGE_SUCCESS};
        Hedge hedge = Hedge.of(HEDGE_ACTIVATION_TIME);
        hedge.getEventPublisher().onPrimarySuccess(event -> logger.info(event.getEventType().toString()));
        Future<String> hedged = hedge.submit(configuredCallable(hedgeBehaviorSpecifications), executor);
        assertThat(hedged.get()).isEqualTo(PRIMARY);
        then(logger).should().info(HedgeEvent.Type.PRIMARY_SUCCESS.toString());
    }

    /**
     * Creates a {@link Callable} of defined behaviors. The first invocation of the callable will return the non-hedged behavior.
     * The second invocation will return the hedged behavior. This includes errors, return values and execution time.
     *
     * @param hedgeBehaviorSpecifications
     * @return the callable with the specified behaviors
     */
    private Callable<String> configuredCallable(HedgeBehaviorSpecification[] hedgeBehaviorSpecifications) {
        AtomicInteger iterations = new AtomicInteger(0);
        return () -> {
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
        };
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