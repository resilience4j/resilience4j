/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.Either;
import org.junit.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.*;
import static io.github.resilience4j.circuitbreaker.utils.CircuitBreakerResultUtils.ifFailedWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

public class CircuitBreakerConfigTest {


    @Test(expected = IllegalArgumentException.class)
    public void zeroMaxFailuresShouldFail() {
        custom().failureRateThreshold(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroWaitIntervalShouldFail() {
        custom().waitDurationInOpenState(Duration.ofMillis(0)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroSlowCallDurationThresholdShouldFail() {
        custom().slowCallDurationThreshold(Duration.ofMillis(0)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPermittedNumberOfCallsInHalfOpenStateShouldFail() {
        custom().permittedNumberOfCallsInHalfOpenState(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroSlidingWindowSizeShouldFail() {
        custom().slidingWindow(
            0,
            0,
            SlidingWindowType.COUNT_BASED,
            SlidingWindowSynchronizationStrategy.SYNCHRONIZED
        ).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroSlidingWindowSizeShouldFail2() {
        custom().slidingWindowSize(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroMinimumNumberOfCallsShouldFail() {
        custom().slidingWindow(
            2,
            0,
            SlidingWindowType.COUNT_BASED,
            SlidingWindowSynchronizationStrategy.SYNCHRONIZED
        ).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroMinimumNumberOfCallsShouldFai2l() {
        custom().minimumNumberOfCalls(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroFailureRateThresholdShouldFail() {
        custom().failureRateThreshold(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroSlowCallRateThresholdShouldFail() {
        custom().slowCallRateThreshold(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failureRateThresholdAboveHundredShouldFail() {
        custom().failureRateThreshold(101).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void slowCallRateThresholdAboveHundredShouldFail() {
        custom().slowCallRateThreshold(101).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void maxWaitDurationInHalfOpenStateLessThanSecondShouldFail() {
        custom().maxWaitDurationInHalfOpenState(Duration.ofMillis(-1)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void transitionToStateAfterWaitDurationNullShouldFail() {
        custom().transitionToStateAfterWaitDuration(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void transitionToStateAfterWaitDurationHalfOpenShouldFail() {
        custom().transitionToStateAfterWaitDuration(CircuitBreaker.State.HALF_OPEN).build();
    }

    @Test
    public void shouldSetDefaultSettings() {
        CircuitBreakerConfig circuitBreakerConfig = ofDefaults();
        then(circuitBreakerConfig.getFailureRateThreshold())
            .isEqualTo(DEFAULT_FAILURE_RATE_THRESHOLD);
        then(circuitBreakerConfig.getPermittedNumberOfCallsInHalfOpenState())
            .isEqualTo(DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE);
        then(circuitBreakerConfig.getSlidingWindowSize()).isEqualTo(DEFAULT_SLIDING_WINDOW_SIZE);
        then(circuitBreakerConfig.getSlidingWindowType()).isEqualTo(DEFAULT_SLIDING_WINDOW_TYPE);
        then(circuitBreakerConfig.getSlidingWindowSynchronizationStrategy())
            .isEqualTo(DEFAULT_SLIDING_WINDOW_SYNCHRONIZATION_STRATEGY);
        then(circuitBreakerConfig.getMinimumNumberOfCalls())
            .isEqualTo(DEFAULT_MINIMUM_NUMBER_OF_CALLS);
        then(circuitBreakerConfig.getWaitIntervalFunctionInOpenState().apply(1))
            .isEqualTo(DEFAULT_SLOW_CALL_DURATION_THRESHOLD * 1000);
        then(circuitBreakerConfig.getRecordExceptionPredicate()).isNotNull();
        then(circuitBreakerConfig.getSlowCallRateThreshold())
            .isEqualTo(DEFAULT_SLOW_CALL_RATE_THRESHOLD);
        then(circuitBreakerConfig.getSlowCallDurationThreshold().getSeconds())
            .isEqualTo(DEFAULT_SLOW_CALL_DURATION_THRESHOLD);
        then(circuitBreakerConfig.isWritableStackTraceEnabled())
            .isEqualTo(DEFAULT_WRITABLE_STACK_TRACE_ENABLED);
        then(circuitBreakerConfig.getTransitionToStateAfterWaitDuration())
            .isEqualTo(DEFAULT_TRANSITION_TO_STATE_AFTER_WAIT_DURATION);
    }

    @Test
    public void shouldSetFailureRateThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = custom().failureRateThreshold(25).build();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(25);
    }


    @Test
    public void shouldSetFailureRateThresholdAsAFloatGreaterThanZero() {
        CircuitBreakerConfig circuitBreakerConfig = custom().failureRateThreshold(0.5f).build();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(0.5f);
    }

    @Test
    public void shouldSetWaitDurationInHalfOpenState() {
        CircuitBreakerConfig circuitBreakerConfig = custom().maxWaitDurationInHalfOpenState(Duration.ofMillis(1000)).build();
        then(circuitBreakerConfig.getMaxWaitDurationInHalfOpenState().toMillis()).isEqualTo(1000);
    }

    @Test
    public void shouldSetClosedStateTransitionToStateAfterWaitDuration() {
        CircuitBreakerConfig circuitBreakerConfig = custom().transitionToStateAfterWaitDuration(CircuitBreaker.State.CLOSED).build();
        then(circuitBreakerConfig.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void shouldSetOpenStateTransitionToStateAfterWaitDuration() {
        CircuitBreakerConfig circuitBreakerConfig = custom().transitionToStateAfterWaitDuration(CircuitBreaker.State.OPEN).build();
        then(circuitBreakerConfig.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    public void shouldSetSlowCallRateThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = custom().slowCallRateThreshold(25).build();
        then(circuitBreakerConfig.getSlowCallRateThreshold()).isEqualTo(25);
    }

    @Test
    public void shouldSetSlowCallRateThresholdAsFloatGreaterThanZero() {
        CircuitBreakerConfig circuitBreakerConfig = custom().slowCallRateThreshold(0.5f).build();
        then(circuitBreakerConfig.getSlowCallRateThreshold()).isEqualTo(0.5f);
    }

    @Test
    public void shouldSetSlowCallDurationThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slowCallDurationThreshold(Duration.ofSeconds(1)).build();
        then(circuitBreakerConfig.getSlowCallDurationThreshold().getSeconds()).isEqualTo(1);
    }

    @Test
    public void shouldSetLowFailureRateThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = custom().failureRateThreshold(0.001f).build();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(0.001f);
    }

    @Test
    public void shouldSetWritableStackTraces() {
        CircuitBreakerConfig circuitBreakerConfig = custom().writableStackTraceEnabled(false)
            .build();
        then(circuitBreakerConfig.isWritableStackTraceEnabled()).isFalse();
    }

    @Test
    public void shouldSetCountBasedSlidingWindowSize() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindow(
                1000,
                1000,
                SlidingWindowType.COUNT_BASED,
                SlidingWindowSynchronizationStrategy.SYNCHRONIZED
            )
            .build();
        then(circuitBreakerConfig.getSlidingWindowSize()).isEqualTo(1000);
    }

    @Test
    public void shouldSetPermittedNumberOfCallsInHalfOpenState() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .permittedNumberOfCallsInHalfOpenState(100).build();
        then(circuitBreakerConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(100);
    }

    @Test
    public void shouldReduceMinimumNumberOfCallsToSlidingWindowSize() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindow(
                5,
                6,
                SlidingWindowType.COUNT_BASED,
                SlidingWindowSynchronizationStrategy.SYNCHRONIZED
            )
            .build();
        then(circuitBreakerConfig.getMinimumNumberOfCalls()).isEqualTo(5);
        then(circuitBreakerConfig.getSlidingWindowSize()).isEqualTo(5);
        then(circuitBreakerConfig.getSlidingWindowType()).isEqualTo(SlidingWindowType.COUNT_BASED);
        then(circuitBreakerConfig.getSlidingWindowSynchronizationStrategy())
            .isEqualTo(SlidingWindowSynchronizationStrategy.SYNCHRONIZED);
    }

    @Test
    public void shouldSetSlidingWindowToCountBased() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindow(
                5,
                3,
                SlidingWindowType.COUNT_BASED,
                SlidingWindowSynchronizationStrategy.SYNCHRONIZED
            )
            .build();
        then(circuitBreakerConfig.getMinimumNumberOfCalls()).isEqualTo(3);
        then(circuitBreakerConfig.getSlidingWindowSize()).isEqualTo(5);
        then(circuitBreakerConfig.getSlidingWindowType()).isEqualTo(SlidingWindowType.COUNT_BASED);
        then(circuitBreakerConfig.getSlidingWindowSynchronizationStrategy())
            .isEqualTo(SlidingWindowSynchronizationStrategy.SYNCHRONIZED);
    }

    @Test
    public void shouldSetSlidingWindowToTimeBased() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindowType(SlidingWindowType.TIME_BASED).build();
        then(circuitBreakerConfig.getSlidingWindowType()).isEqualTo(SlidingWindowType.TIME_BASED);
    }

    @Test
    public void shouldSetSlidingWindowSize() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindowSize(1000).build();
        then(circuitBreakerConfig.getSlidingWindowSize()).isEqualTo(1000);
    }

    @Test
    public void shouldSetMinimumNumberOfCalls() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .minimumNumberOfCalls(1000).build();
        then(circuitBreakerConfig.getMinimumNumberOfCalls()).isEqualTo(1000);
    }

    @Test
    public void shouldSetSlidingWindowSynchronizationStrategyToLockFree() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindowSynchronizationStrategy(SlidingWindowSynchronizationStrategy.LOCK_FREE).build();
        then(circuitBreakerConfig.getSlidingWindowSynchronizationStrategy())
            .isEqualTo(SlidingWindowSynchronizationStrategy.LOCK_FREE);
    }

    @Test
    public void maxWaitDurationInHalfOpenStateEqualZeroShouldPass() {
        CircuitBreakerConfig circuitBreakerConfig = custom().maxWaitDurationInHalfOpenState(Duration.ZERO).build();
        then(circuitBreakerConfig.getMaxWaitDurationInHalfOpenState().getSeconds()).isEqualTo(0);
    }

    @Test
    public void shouldAllowHighMinimumNumberOfCallsWhenSlidingWindowIsTimeBased() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindow(
                10,
                100,
                SlidingWindowType.TIME_BASED,
                SlidingWindowSynchronizationStrategy.SYNCHRONIZED
            )
            .build();
        then(circuitBreakerConfig.getMinimumNumberOfCalls()).isEqualTo(100);
        then(circuitBreakerConfig.getSlidingWindowSize()).isEqualTo(10);
        then(circuitBreakerConfig.getSlidingWindowType()).isEqualTo(SlidingWindowType.TIME_BASED);
    }

    @Test
    public void shouldSetWaitInterval() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .waitDurationInOpenState(Duration.ofSeconds(1)).build();
        then(circuitBreakerConfig.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(1000);
    }

    @Test
    public void shouldUseCustomRecordExceptionPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .recordException(e -> "test".equals(e.getMessage())).build();
        Predicate<Throwable> recordExceptionPredicate = circuitBreakerConfig
            .getRecordExceptionPredicate();
        then(recordExceptionPredicate.test(new Error("test"))).isTrue();
        then(recordExceptionPredicate.test(new Error("fail"))).isFalse();
        then(recordExceptionPredicate.test(new RuntimeException("test"))).isTrue();
        then(recordExceptionPredicate.test(new Error())).isFalse();
        then(recordExceptionPredicate.test(new RuntimeException())).isFalse();
    }

    @Test
    public void shouldUseCustomIgnoreExceptionPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .ignoreException(e -> "ignore".equals(e.getMessage())).build();
        Predicate<Throwable> ignoreExceptionPredicate = circuitBreakerConfig
            .getIgnoreExceptionPredicate();
        then(ignoreExceptionPredicate.test(new Error("ignore"))).isTrue();
        then(ignoreExceptionPredicate.test(new Error("fail"))).isFalse();
        then(ignoreExceptionPredicate.test(new RuntimeException("ignore"))).isTrue();
        then(ignoreExceptionPredicate.test(new Error())).isFalse();
        then(ignoreExceptionPredicate.test(new RuntimeException())).isFalse();
    }

    @Test
    public void shouldUseIgnoreExceptionsToBuildPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .ignoreExceptions(RuntimeException.class, ExtendsExtendsException.class,
                BusinessException.class).build();
        final Predicate<? super Throwable> ignoreExceptionPredicate = circuitBreakerConfig
            .getIgnoreExceptionPredicate();
        then(ignoreExceptionPredicate.test(new Exception()))
            .isFalse(); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsError()))
            .isFalse(); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsException()))
            .isFalse(); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new BusinessException()))
            .isTrue(); // explicitly ignored
        then(ignoreExceptionPredicate.test(new RuntimeException()))
            .isTrue(); // explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsRuntimeException()))
            .isTrue(); // inherits ignored because of RuntimeException is ignored
        then(ignoreExceptionPredicate.test(new ExtendsExtendsException()))
            .isTrue(); // explicitly ignored
    }

    @Test
    public void shouldUseRecordExceptionsToBuildPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .recordExceptions(RuntimeException.class, ExtendsExtendsException.class).build();
        final Predicate<? super Throwable> failurePredicate = circuitBreakerConfig
            .getRecordExceptionPredicate();
        then(failurePredicate.test(new Exception())).isFalse(); // not explicitly record
        then(failurePredicate.test(new ExtendsError())).isFalse(); // not explicitly included
        then(failurePredicate.test(new ExtendsException()))
            .isFalse(); // not explicitly included
        then(failurePredicate.test(new BusinessException()))
            .isFalse(); // not explicitly included
        then(failurePredicate.test(new RuntimeException())).isTrue(); // explicitly included
        then(failurePredicate.test(new ExtendsRuntimeException()))
            .isTrue(); // inherits included because RuntimeException is included
        then(failurePredicate.test(new ExtendsExtendsException()))
            .isTrue(); // explicitly included
    }

    @Test
    public void shouldCreateCombinedRecordExceptionPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .recordException(e -> "test".equals(e.getMessage())) //1
            .recordExceptions(RuntimeException.class, ExtendsExtendsException.class) //2
            .build();
        final Predicate<? super Throwable> recordExceptionPredicate = circuitBreakerConfig
            .getRecordExceptionPredicate();
        then(recordExceptionPredicate.test(new Exception()))
            .isFalse(); // not explicitly included
        then(recordExceptionPredicate.test(new Exception("test")))
            .isTrue(); // explicitly included by 1
        then(recordExceptionPredicate.test(new ExtendsError()))
            .isFalse(); // not explicitly included
        then(recordExceptionPredicate.test(new ExtendsException()))
            .isFalse();  // explicitly excluded by 3
        then(recordExceptionPredicate.test(new ExtendsException("test")))
            .isTrue();  // explicitly included by 1
        then(recordExceptionPredicate.test(new BusinessException()))
            .isFalse(); // not explicitly included
        then(recordExceptionPredicate.test(new RuntimeException()))
            .isTrue(); // explicitly included by 2
        then(recordExceptionPredicate.test(new ExtendsRuntimeException()))
            .isTrue(); // implicitly included by RuntimeException
        then(recordExceptionPredicate.test(new ExtendsExtendsException()))
            .isTrue(); // explicitly included
    }

    @Test
    public void shouldCreateWaitIntervalPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .waitIntervalFunctionInOpenState((i) -> (long) i)
            .build();
        IntervalFunction intervalFunction =
            circuitBreakerConfig.getWaitIntervalFunctionInOpenState();
        then(intervalFunction).isNotNull();
        for (int i = 0; i < 10; i++) {
            then(intervalFunction.apply(i)).isEqualTo(i);
        }
    }

    @Test
    public void shouldCreateTransitionCheckFunction() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .transitionOnResult(ifFailedWith(AccessBanException.class)
                .thenOpenUntil(AccessBanException::getBannedUntil))
            .build();
        Function<Either<Object, Throwable>, TransitionCheckResult> transitionCheck =
            circuitBreakerConfig.getTransitionOnResult();
        then(transitionCheck).isNotNull();
        then(transitionCheck
            .apply(Either.right(new AccessBanException(Instant.parse("2007-12-03T10:15:30.00Z"))))
            .getWaitUntil())
            .isEqualTo("2007-12-03T10:15:30.00Z");
    }

    @Test
    public void shouldCreateCurrentTimeFunction() {
        Instant instant = Instant.now();
        Clock fixedClock = Clock.fixed(instant, ZoneId.systemDefault());
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .currentTimestampFunction(clock -> clock.instant().toEpochMilli(), TimeUnit.MILLISECONDS)
            .build();
        final Function<Clock, Long> currentTimeFunction = circuitBreakerConfig.getCurrentTimestampFunction();
        then(currentTimeFunction).isNotNull();
        then(currentTimeFunction.apply(fixedClock)).isEqualTo(instant.toEpochMilli());
    }

    @Test
    public void shouldCreateCombinedIgnoreExceptionPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .ignoreException(e -> "ignore".equals(e.getMessage())) //1
            .ignoreExceptions(BusinessException.class, ExtendsExtendsException.class,
                ExtendsRuntimeException.class) //2
            .build();
        final Predicate<? super Throwable> ignoreExceptionPredicate = circuitBreakerConfig
            .getIgnoreExceptionPredicate();
        then(ignoreExceptionPredicate.test(new Exception()))
            .isFalse(); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new Exception("ignore")))
            .isTrue(); // explicitly ignored by 1
        then(ignoreExceptionPredicate.test(new ExtendsError()))
            .isFalse(); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsException()))
            .isFalse();  // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsException("ignore")))
            .isTrue();  // explicitly ignored 1
        then(ignoreExceptionPredicate.test(new BusinessException()))
            .isTrue(); // explicitly ignored 2
        then(ignoreExceptionPredicate.test(new RuntimeException()))
            .isFalse(); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsRuntimeException()))
            .isTrue(); // explicitly ignored 2
        then(ignoreExceptionPredicate.test(new ExtendsExtendsException()))
            .isTrue(); // implicitly ignored by ExtendsRuntimeException
    }

    @Test
    public void shouldBuilderCreateConfigEveryTime() {
        final Builder builder = custom();
        builder.slidingWindowSize(5);
        final CircuitBreakerConfig config1 = builder.build();
        builder.slidingWindowSize(3);
        final CircuitBreakerConfig config2 = builder.build();
        assertThat(config2.getSlidingWindowSize()).isEqualTo(3);
        assertThat(config1.getSlidingWindowSize()).isEqualTo(5);
    }

    @Test
    public void shouldUseBaseConfigAndOverwriteProperties() {
        CircuitBreakerConfig baseConfig = custom()
            .waitDurationInOpenState(Duration.ofSeconds(100))
            .slidingWindowSize(1000)
            .slidingWindowType(SlidingWindowType.TIME_BASED)
            .slidingWindowSynchronizationStrategy(SlidingWindowSynchronizationStrategy.LOCK_FREE)
            .permittedNumberOfCallsInHalfOpenState(100)
            .writableStackTraceEnabled(false)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .failureRateThreshold(20f).build();

        CircuitBreakerConfig extendedConfig = from(baseConfig)
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .build();

        then(extendedConfig.getFailureRateThreshold()).isEqualTo(20f);
        then(extendedConfig.isWritableStackTraceEnabled()).isFalse();
        then(extendedConfig.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(20_000);
        then(extendedConfig.getSlidingWindowSize()).isEqualTo(1000);
        then(extendedConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(100);
        then(extendedConfig.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
        then(extendedConfig.getSlidingWindowType()).isEqualTo(SlidingWindowType.TIME_BASED);
        then(extendedConfig.getSlidingWindowSynchronizationStrategy())
            .isEqualTo(SlidingWindowSynchronizationStrategy.LOCK_FREE);
    }

    @Test
    public void testToString() {
        CircuitBreakerConfig config = custom()
                .slidingWindowSize(5)
                .recordExceptions(RuntimeException.class)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .slidingWindowType(SlidingWindowType.TIME_BASED).build();

        String result = config.toString();

        assertThat(result).startsWith("CircuitBreakerConfig {");
        assertThat(result).contains("slidingWindowSize=5");
        assertThat(result).contains("recordExceptions=[class java.lang.RuntimeException]");
        assertThat(result).contains("automaticTransitionFromOpenToHalfOpenEnabled=true");
        assertThat(result).contains("slidingWindowType=TIME_BASED");
        assertThat(result).contains("slidingWindowSynchronizationStrategy=SYNCHRONIZED");
        assertThat(result).endsWith("}");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUseWitIntervalFunctionInOpenStateAndWaitDurationInOpenStateTogether() {
        custom()
            .waitDurationInOpenState(Duration.ofMillis(3333))
            .waitIntervalFunctionInOpenState(IntervalFunction.of(Duration.ofMillis(1234)))
            .build();
    }

    @Test
    public void testOverrideIntervalFunction() {
        assertThat(custom()
            .waitIntervalFunctionInOpenState(IntervalFunction.of(Duration.ofMillis(1234)))
            .build()).isNotNull();
        assertThat(custom()
            .waitDurationInOpenState(Duration.ofMillis(3333))
            .build()).isNotNull();

        final CircuitBreakerConfig custom = custom().waitDurationInOpenState(Duration.ofMillis(23434)).build();
        CircuitBreakerConfig build = from(custom)
            .waitIntervalFunctionInOpenState(IntervalFunction.of(Duration.ofMillis(1234)))
            .build();

        assertThat(build.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(1234);

        build = from(custom)
            .waitDurationInOpenState(Duration.ofMillis(1234))
            .build();
        assertThat(build.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(1234);

        build = from(custom).build();
        assertThat(build.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(23434);
    }

    private static class ExtendsException extends Exception {

        ExtendsException() {
        }

        ExtendsException(String message) {
            super(message);
        }
    }

    private static class ExtendsRuntimeException extends RuntimeException {

    }

    private static class ExtendsExtendsException extends ExtendsException {

    }

    private static class BusinessException extends Exception {

    }

    private static class AccessBanException extends Exception {

        private final Instant bannedUntil;

        public AccessBanException(Instant bannedUntil) {
            this.bannedUntil = bannedUntil;
        }

        public Instant getBannedUntil() {
            return bannedUntil;
        }
    }

    private static class ExtendsError extends Error {

    }

}
