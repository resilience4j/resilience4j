/*
 *
 *  Copyright 2026 Robert Winkler
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.Builder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowSynchronizationStrategy;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.TransitionCheckResult;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_FAILURE_RATE_THRESHOLD;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_MINIMUM_NUMBER_OF_CALLS;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_SIZE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_SYNCHRONIZATION_STRATEGY;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_TYPE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLOW_CALL_DURATION_THRESHOLD;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLOW_CALL_RATE_THRESHOLD;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_TRANSITION_TO_STATE_AFTER_WAIT_DURATION;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.from;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.ofDefaults;
import static io.github.resilience4j.circuitbreaker.utils.CircuitBreakerResultUtils.ifFailedWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

class CircuitBreakerConfigTest {


    @Test
    void zeroMaxFailuresShouldFail() {
        assertThatThrownBy(() -> custom().failureRateThreshold(0).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroWaitIntervalShouldFail() {
        assertThatThrownBy(() -> custom().waitDurationInOpenState(Duration.ofMillis(0)).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroSlowCallDurationThresholdShouldFail() {
        assertThatThrownBy(() -> custom().slowCallDurationThreshold(Duration.ofMillis(0)).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroPermittedNumberOfCallsInHalfOpenStateShouldFail() {
        assertThatThrownBy(() -> custom().permittedNumberOfCallsInHalfOpenState(0).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
     void zeroSlidingWindowSizeShouldFail() {
         assertThatThrownBy(() -> custom().slidingWindow(
             0, 0, SlidingWindowType.COUNT_BASED, SlidingWindowSynchronizationStrategy.SYNCHRONIZED
         ).build()).isInstanceOf(IllegalArgumentException.class);
     }

    @Test
    void zeroSlidingWindowSizeShouldFail2() {
        assertThatThrownBy(() -> custom().slidingWindowSize(0).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroMinimumNumberOfCallsShouldFail() {
        assertThatThrownBy(() -> custom().slidingWindow(
            2, 0, SlidingWindowType.COUNT_BASED, SlidingWindowSynchronizationStrategy.SYNCHRONIZED
        ).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroMinimumNumberOfCallsShouldFai2l() {
        assertThatThrownBy(() -> custom().minimumNumberOfCalls(0).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroFailureRateThresholdShouldFail() {
        assertThatThrownBy(() -> custom().failureRateThreshold(0).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroSlowCallRateThresholdShouldFail() {
        assertThatThrownBy(() -> custom().slowCallRateThreshold(0).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failureRateThresholdAboveHundredShouldFail() {
        assertThatThrownBy(() -> custom().failureRateThreshold(101).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void slowCallRateThresholdAboveHundredShouldFail() {
        assertThatThrownBy(() -> custom().slowCallRateThreshold(101).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maxWaitDurationInHalfOpenStateLessThanSecondShouldFail() {
        assertThatThrownBy(() -> custom().maxWaitDurationInHalfOpenState(Duration.ofMillis(-1)).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transitionToStateAfterWaitDurationNullShouldFail() {
        assertThatThrownBy(() -> custom().transitionToStateAfterWaitDuration(null).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transitionToStateAfterWaitDurationHalfOpenShouldFail() {
        assertThatThrownBy(() -> custom().transitionToStateAfterWaitDuration(CircuitBreaker.State.HALF_OPEN).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSetDefaultSettings() {
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
    void shouldSetFailureRateThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = custom().failureRateThreshold(25).build();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(25);
    }


    @Test
    void shouldSetFailureRateThresholdAsAFloatGreaterThanZero() {
        CircuitBreakerConfig circuitBreakerConfig = custom().failureRateThreshold(0.5f).build();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(0.5f);
    }

    @Test
    void shouldSetWaitDurationInHalfOpenState() {
        CircuitBreakerConfig circuitBreakerConfig = custom().maxWaitDurationInHalfOpenState(Duration.ofMillis(1000)).build();
        then(circuitBreakerConfig.getMaxWaitDurationInHalfOpenState().toMillis()).isEqualTo(1000);
    }

    @Test
    void shouldSetClosedStateTransitionToStateAfterWaitDuration() {
        CircuitBreakerConfig circuitBreakerConfig = custom().transitionToStateAfterWaitDuration(CircuitBreaker.State.CLOSED).build();
        then(circuitBreakerConfig.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void shouldSetOpenStateTransitionToStateAfterWaitDuration() {
        CircuitBreakerConfig circuitBreakerConfig = custom().transitionToStateAfterWaitDuration(CircuitBreaker.State.OPEN).build();
        then(circuitBreakerConfig.getTransitionToStateAfterWaitDuration()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldSetSlowCallRateThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = custom().slowCallRateThreshold(25).build();
        then(circuitBreakerConfig.getSlowCallRateThreshold()).isEqualTo(25);
    }

    @Test
    void shouldSetSlowCallRateThresholdAsFloatGreaterThanZero() {
        CircuitBreakerConfig circuitBreakerConfig = custom().slowCallRateThreshold(0.5f).build();
        then(circuitBreakerConfig.getSlowCallRateThreshold()).isEqualTo(0.5f);
    }

    @Test
    void shouldSetSlowCallDurationThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slowCallDurationThreshold(Duration.ofSeconds(1)).build();
        then(circuitBreakerConfig.getSlowCallDurationThreshold().getSeconds()).isOne();
    }

    @Test
    void shouldSetLowFailureRateThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = custom().failureRateThreshold(0.001f).build();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(0.001f);
    }

    @Test
    void shouldSetWritableStackTraces() {
        CircuitBreakerConfig circuitBreakerConfig = custom().writableStackTraceEnabled(false)
            .build();
        then(circuitBreakerConfig.isWritableStackTraceEnabled()).isFalse();
    }

    @Test
    void shouldSetCountBasedSlidingWindowSize() {
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
    void shouldSetPermittedNumberOfCallsInHalfOpenState() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .permittedNumberOfCallsInHalfOpenState(100).build();
        then(circuitBreakerConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(100);
    }

    @Test
    void shouldReduceMinimumNumberOfCallsToSlidingWindowSize() {
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
    void shouldSetSlidingWindowToCountBased() {
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
    void shouldSetSlidingWindowToTimeBased() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindowType(SlidingWindowType.TIME_BASED).build();
        then(circuitBreakerConfig.getSlidingWindowType()).isEqualTo(SlidingWindowType.TIME_BASED);
    }

    @Test
    void shouldSetSlidingWindowSize() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindowSize(1000).build();
        then(circuitBreakerConfig.getSlidingWindowSize()).isEqualTo(1000);
    }

    @Test
    void shouldSetMinimumNumberOfCalls() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .minimumNumberOfCalls(1000).build();
        then(circuitBreakerConfig.getMinimumNumberOfCalls()).isEqualTo(1000);
    }

    @Test
    void shouldSetSlidingWindowSynchronizationStrategyToLockFree() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .slidingWindowSynchronizationStrategy(SlidingWindowSynchronizationStrategy.LOCK_FREE).build();
        then(circuitBreakerConfig.getSlidingWindowSynchronizationStrategy())
            .isEqualTo(SlidingWindowSynchronizationStrategy.LOCK_FREE);
    }

    @Test
    void maxWaitDurationInHalfOpenStateEqualZeroShouldPass() {
        CircuitBreakerConfig circuitBreakerConfig = custom().maxWaitDurationInHalfOpenState(Duration.ZERO).build();
        then(circuitBreakerConfig.getMaxWaitDurationInHalfOpenState().getSeconds()).isZero();
    }

    @Test
    void shouldAllowHighMinimumNumberOfCallsWhenSlidingWindowIsTimeBased() {
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
    void shouldUseSynchronizedStrategyByDefault() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
                .slidingWindow(
                        1000,
                        1000,
                        SlidingWindowType.COUNT_BASED
                )
                .build();
        then(circuitBreakerConfig.getSlidingWindowSize()).isEqualTo(1000);
        then(circuitBreakerConfig.getSlidingWindowSynchronizationStrategy())
                .isEqualTo(SlidingWindowSynchronizationStrategy.SYNCHRONIZED);
    }

    @Test
    void shouldSetWaitInterval() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
            .waitDurationInOpenState(Duration.ofSeconds(1)).build();
        then(circuitBreakerConfig.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(1000);
    }

    @Test
    void shouldUseCustomRecordExceptionPredicate() {
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
    void shouldUseCustomIgnoreExceptionPredicate() {
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
    void shouldUseIgnoreExceptionsToBuildPredicate() {
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
    void shouldUseRecordExceptionsToBuildPredicate() {
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
    void shouldCreateCombinedRecordExceptionPredicate() {
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
    void shouldCreateWaitIntervalPredicate() {
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
    void shouldCreateTransitionCheckFunction() {
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
    void shouldCreateCurrentTimeFunction() {
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
    void shouldCreateCombinedIgnoreExceptionPredicate() {
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
    void shouldBuilderCreateConfigEveryTime() {
        final Builder builder = custom();
        builder.slidingWindowSize(5);
        final CircuitBreakerConfig config1 = builder.build();
        builder.slidingWindowSize(3);
        final CircuitBreakerConfig config2 = builder.build();
        assertThat(config2.getSlidingWindowSize()).isEqualTo(3);
        assertThat(config1.getSlidingWindowSize()).isEqualTo(5);
    }

    @Test
    void shouldUseBaseConfigAndOverwriteProperties() {
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
    void testToString() {
        CircuitBreakerConfig config = custom()
                .slidingWindowSize(5)
                .recordExceptions(RuntimeException.class)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .slidingWindowType(SlidingWindowType.TIME_BASED).build();

        String result = config.toString();

        assertThat(result)
                .startsWith("CircuitBreakerConfig {")
                .contains("slidingWindowSize=5")
                .contains("recordExceptions=[class java.lang.RuntimeException]")
                .contains("automaticTransitionFromOpenToHalfOpenEnabled=true")
                .contains("slidingWindowType=TIME_BASED")
                .contains("slidingWindowSynchronizationStrategy=SYNCHRONIZED")
                .endsWith("}");
    }

    @Test
    void shouldNotUseWitIntervalFunctionInOpenStateAndWaitDurationInOpenStateTogether() {
        assertThatThrownBy(() -> custom()
                .waitDurationInOpenState(Duration.ofMillis(3333))
                .waitIntervalFunctionInOpenState(IntervalFunction.of(Duration.ofMillis(1234)))
                .build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void overrideIntervalFunction() {
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


    @Test
    void shouldSetIgnoreExceptionsPrecedenceEnabled() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
                .ignoreExceptionsPrecedenceEnabled(true)
                .build();
        then(circuitBreakerConfig.isIgnoreExceptionsPrecedenceEnabled()).isTrue();
    }

    @Test
    void shouldSetIgnoreExceptionsPrecedenceEnabledToFalse() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
                .ignoreExceptionsPrecedenceEnabled(false)
                .build();
        then(circuitBreakerConfig.isIgnoreExceptionsPrecedenceEnabled()).isFalse();
    }

    @Test
    void shouldEnableIgnoreExceptionsPrecedence() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
                .enableIgnoreExceptionsPrecedence()
                .build();
        then(circuitBreakerConfig.isIgnoreExceptionsPrecedenceEnabled()).isTrue();
    }

    @Test
    void shouldRespectIgnoreExceptionsWhenPrecedenceEnabled() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
                .recordExceptions(Exception.class)
                .ignoreExceptions(RuntimeException.class, ExtendsException.class)
                .enableIgnoreExceptionsPrecedence()
                .build();

        final Predicate<? super Throwable> recordExceptionPredicate =
                circuitBreakerConfig.getRecordExceptionPredicate();
        final Predicate<? super Throwable> ignoreExceptionPredicate =
                circuitBreakerConfig.getIgnoreExceptionPredicate();

        then(ignoreExceptionPredicate.test(new BusinessException())).isFalse();
        then(recordExceptionPredicate.test(new BusinessException())).isTrue();
        then(ignoreExceptionPredicate.test(new RuntimeException())).isTrue();
        then(recordExceptionPredicate.test(new RuntimeException())).isFalse();
        then(ignoreExceptionPredicate.test(new ExtendsRuntimeException())).isTrue();
        then(recordExceptionPredicate.test(new ExtendsRuntimeException())).isFalse();
        then(ignoreExceptionPredicate.test(new ExtendsException())).isTrue();
        then(recordExceptionPredicate.test(new ExtendsException())).isFalse();
        then(ignoreExceptionPredicate.test(new ExtendsExtendsException())).isTrue();
        then(recordExceptionPredicate.test(new ExtendsExtendsException())).isFalse();
    }

    @Test
    void shouldRespectRecordExceptionsWhenPrecedenceDisabled() {
        CircuitBreakerConfig circuitBreakerConfig = custom()
                .recordExceptions(Exception.class)
                .ignoreExceptions(RuntimeException.class, ExtendsException.class)
                .build();

        final Predicate<? super Throwable> recordExceptionPredicate =
                circuitBreakerConfig.getRecordExceptionPredicate();
        final Predicate<? super Throwable> ignoreExceptionPredicate =
                circuitBreakerConfig.getIgnoreExceptionPredicate();

        then(ignoreExceptionPredicate.test(new BusinessException())).isFalse();
        then(recordExceptionPredicate.test(new BusinessException())).isTrue();
        then(ignoreExceptionPredicate.test(new RuntimeException())).isTrue();
        then(recordExceptionPredicate.test(new RuntimeException())).isTrue();
        then(ignoreExceptionPredicate.test(new ExtendsRuntimeException())).isTrue();
        then(recordExceptionPredicate.test(new ExtendsRuntimeException())).isTrue();
        then(ignoreExceptionPredicate.test(new ExtendsException())).isTrue();
        then(recordExceptionPredicate.test(new ExtendsException())).isTrue();
        then(ignoreExceptionPredicate.test(new ExtendsExtendsException())).isTrue();
        then(recordExceptionPredicate.test(new ExtendsExtendsException())).isTrue();
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
