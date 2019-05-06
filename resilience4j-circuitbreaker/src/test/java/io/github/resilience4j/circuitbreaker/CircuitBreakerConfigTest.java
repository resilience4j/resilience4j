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

import org.junit.Test;

import java.time.Duration;
import java.util.function.Predicate;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CircuitBreakerConfigTest {

    private static final Predicate<Throwable> TEST_PREDICATE = e -> "test".equals(e.getMessage());

    @Test(expected = IllegalArgumentException.class)
    public void zeroMaxFailuresShouldFail() {
        CircuitBreakerConfig.custom().failureRateThreshold(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroWaitIntervalShouldFail() {
        CircuitBreakerConfig.custom().waitDurationInOpenState(Duration.ofMillis(0)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void ringBufferSizeInHalfOpenStateBelowOneShouldFail() {
        CircuitBreakerConfig.custom().ringBufferSizeInHalfOpenState(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void ringBufferSizeInClosedStateBelowOneThenShouldFail() {
        CircuitBreakerConfig.custom().ringBufferSizeInClosedState(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroFailureRateThresholdShouldFail() {
        CircuitBreakerConfig.custom().failureRateThreshold(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failureRateThresholdAboveHundredShouldFail() {
        CircuitBreakerConfig.custom().failureRateThreshold(101).build();
    }

    @Test
    public void shouldSetDefaultSettings() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(CircuitBreakerConfig.DEFAULT_MAX_FAILURE_THRESHOLD);
        then(circuitBreakerConfig.getRingBufferSizeInHalfOpenState()).isEqualTo(CircuitBreakerConfig.DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE);
        then(circuitBreakerConfig.getRingBufferSizeInClosedState()).isEqualTo(CircuitBreakerConfig.DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE);
        then(circuitBreakerConfig.getWaitDurationInOpenState().getSeconds()).isEqualTo(CircuitBreakerConfig.DEFAULT_WAIT_DURATION_IN_OPEN_STATE);
        then(circuitBreakerConfig.getRecordFailurePredicate()).isNotNull();
    }

    @Test
    public void shouldSetFailureRateThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().failureRateThreshold(25).build();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(25);
    }

    @Test
    public void shouldSetLowFailureRateThreshold() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().failureRateThreshold(0.001f).build();
        then(circuitBreakerConfig.getFailureRateThreshold()).isEqualTo(0.001f);
    }

    @Test
    public void shouldSetRingBufferSizeInClosedState() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().ringBufferSizeInClosedState(1000).build();
        then(circuitBreakerConfig.getRingBufferSizeInClosedState()).isEqualTo(1000);
    }

    @Test
    public void shouldSetRingBufferSizeInHalfOpenState() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().ringBufferSizeInHalfOpenState(100).build();
        then(circuitBreakerConfig.getRingBufferSizeInHalfOpenState()).isEqualTo(100);
    }

    @Test
    public void shouldSetWaitInterval() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().waitDurationInOpenState(Duration.ofSeconds(1)).build();
        then(circuitBreakerConfig.getWaitDurationInOpenState().getSeconds()).isEqualTo(1);
    }

    @Test
    public void shouldUseRecordFailureThrowablePredicate() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .recordFailure(TEST_PREDICATE).build();
        then(circuitBreakerConfig.getRecordFailurePredicate().test(new Error("test"))).isEqualTo(true);
        then(circuitBreakerConfig.getRecordFailurePredicate().test(new Error("fail"))).isEqualTo(false);
        then(circuitBreakerConfig.getRecordFailurePredicate().test(new RuntimeException("test"))).isEqualTo(true);
        then(circuitBreakerConfig.getRecordFailurePredicate().test(new Error())).isEqualTo(false);
        then(circuitBreakerConfig.getRecordFailurePredicate().test(new RuntimeException())).isEqualTo(false);
    }

    private static class ExtendsException extends Exception {
        ExtendsException() { }
        ExtendsException(String message) { super(message); }
    }
    private static class ExtendsRuntimeException extends RuntimeException {}
    private static class ExtendsExtendsException extends ExtendsException {}
    private static class ExtendsException2 extends Exception {}
    private static class ExtendsError extends Error {}

    @Test
    public void shouldUseIgnoreExceptionToBuildPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .ignoreExceptions(RuntimeException.class, ExtendsExtendsException.class).build();
        final Predicate<? super Throwable> failurePredicate = circuitBreakerConfig.getRecordFailurePredicate();
        then(failurePredicate.test(new Exception())).isEqualTo(true); // not explicitly excluded
        then(failurePredicate.test(new ExtendsError())).isEqualTo(true); // not explicitly excluded
        then(failurePredicate.test(new ExtendsException())).isEqualTo(true); // not explicitly excluded
        then(failurePredicate.test(new ExtendsException2())).isEqualTo(true); // not explicitly excluded
        then(failurePredicate.test(new RuntimeException())).isEqualTo(false); // explicitly excluded
        then(failurePredicate.test(new ExtendsRuntimeException())).isEqualTo(false); // inherits excluded from ExtendsException
        then(failurePredicate.test(new ExtendsExtendsException())).isEqualTo(false); // explicitly excluded
    }

    @Test
    public void shouldUseRecordExceptionToBuildPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .recordExceptions(RuntimeException.class, ExtendsExtendsException.class).build();
        final Predicate<? super Throwable> failurePredicate = circuitBreakerConfig.getRecordFailurePredicate();
        then(failurePredicate.test(new Exception())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsError())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsException())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsException2())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new RuntimeException())).isEqualTo(true); // explicitly included
        then(failurePredicate.test(new ExtendsRuntimeException())).isEqualTo(true); // inherits included from ExtendsException
        then(failurePredicate.test(new ExtendsExtendsException())).isEqualTo(true); // explicitly included
    }

    @Test
    public void shouldUseIgnoreExceptionOverRecordToBuildPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .recordExceptions(RuntimeException.class, ExtendsExtendsException.class)
                .ignoreExceptions(ExtendsException.class, ExtendsRuntimeException.class)
                .build();
        final Predicate<? super Throwable> failurePredicate = circuitBreakerConfig.getRecordFailurePredicate();
        then(failurePredicate.test(new Exception())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsError())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsException())).isEqualTo(false);  // explicitly excluded
        then(failurePredicate.test(new ExtendsException2())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new RuntimeException())).isEqualTo(true); // explicitly included
        then(failurePredicate.test(new ExtendsRuntimeException())).isEqualTo(false); // explicitly excluded
        then(failurePredicate.test(new ExtendsExtendsException())).isEqualTo(false); // inherits excluded from ExtendsException
    }

    @Test
    public void shouldUseBothRecordToBuildPredicate() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .recordFailure(TEST_PREDICATE) //1
                .recordExceptions(RuntimeException.class, ExtendsExtendsException.class) //2
                .ignoreExceptions(ExtendsException.class, ExtendsRuntimeException.class) //3
                .build();
        final Predicate<? super Throwable> failurePredicate = circuitBreakerConfig.getRecordFailurePredicate();
        then(failurePredicate.test(new Exception())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new Exception("test"))).isEqualTo(true); // explicitly included by 1
        then(failurePredicate.test(new ExtendsError())).isEqualTo(false); // ot explicitly included
        then(failurePredicate.test(new ExtendsException())).isEqualTo(false);  // explicitly excluded by 3
        then(failurePredicate.test(new ExtendsException("test"))).isEqualTo(false);  // explicitly excluded by 3 even if included by 1
        then(failurePredicate.test(new ExtendsException2())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new RuntimeException())).isEqualTo(true); // explicitly included by 2
        then(failurePredicate.test(new ExtendsRuntimeException())).isEqualTo(false); // explicitly excluded by 3
        then(failurePredicate.test(new ExtendsExtendsException())).isEqualTo(false); // inherits excluded from ExtendsException by 3
    }

    @Test
    public void shouldBuilderCreateConfigEveryTime() {
        final CircuitBreakerConfig.Builder builder =  CircuitBreakerConfig.custom();
        builder.ringBufferSizeInClosedState(5);
        final CircuitBreakerConfig config1 = builder.build();
        builder.ringBufferSizeInClosedState(3);
        final CircuitBreakerConfig config2 = builder.build();
        assertThat(config2.getRingBufferSizeInClosedState()).isEqualTo(3);
        assertThat(config1.getRingBufferSizeInClosedState()).isEqualTo(5);
    }

    @Test
    public void shouldUseBaseConfigAndOverwriteProperties() {
        CircuitBreakerConfig baseConfig = CircuitBreakerConfig.custom()
                .waitDurationInOpenState(Duration.ofSeconds(100))
                .ringBufferSizeInClosedState(1000)
                .ringBufferSizeInHalfOpenState(100)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .failureRateThreshold(20f).build();

        CircuitBreakerConfig extendedConfig = CircuitBreakerConfig.from(baseConfig)
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .build();

        then(extendedConfig.getFailureRateThreshold()).isEqualTo(20f);
        then(extendedConfig.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(20));
        then(extendedConfig.getRingBufferSizeInClosedState()).isEqualTo(1000);
        then(extendedConfig.getRingBufferSizeInHalfOpenState()).isEqualTo(100);
        then(extendedConfig.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
    }

}
