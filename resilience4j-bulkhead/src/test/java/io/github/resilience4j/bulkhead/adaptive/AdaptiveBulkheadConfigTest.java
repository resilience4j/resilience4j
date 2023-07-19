package io.github.resilience4j.bulkhead.adaptive;

import org.junit.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

public class AdaptiveBulkheadConfigTest {

    private static final AdaptiveBulkheadConfig DEFAULT_BULKHEAD = AdaptiveBulkheadConfig.ofDefaults();

    @Test
    public void testBuildCustom() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .decreaseMultiplier(0.3f)
            .increaseMultiplier(1.3f)
            .minConcurrentCalls(3)
            .maxConcurrentCalls(3)
            .slowCallDurationThreshold(Duration.ofMillis(150))
            .slowCallRateThreshold(50)
            .failureRateThreshold(50)
            .maxWaitDuration(Duration.ofMillis(150))
            .slidingWindowSize(100)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getDecreaseMultiplier()).isEqualTo(0.3f);
        assertThat(config.getIncreaseMultiplier()).isEqualTo(1.3f);
        assertThat(config.getMinConcurrentCalls()).isEqualTo(3);
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(config.getMaxWaitDuration().toMillis()).isEqualTo(150);
        assertThat(config.getSlidingWindowSize()).isEqualTo(100);
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(50);
        assertThat(config.getFailureRateThreshold()).isEqualTo(50);
        assertThat(config.getSlidingWindowType()).isEqualTo(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED);
    }

    @Test
    public void testDecreaseMultiplierConfig() {
        float decreaseMultiplier = 0.85f;
        assertThat(AdaptiveBulkheadConfig.custom()
            .decreaseMultiplier(decreaseMultiplier)
            .build()
            .getDecreaseMultiplier())
            .isEqualTo(decreaseMultiplier)
            .isNotEqualTo(DEFAULT_BULKHEAD.getDecreaseMultiplier());
    }

    @Test
    public void testInvalidDecreaseMultiplierConfig() {
        assertThatThrownBy(() -> AdaptiveBulkheadConfig.custom()
            .decreaseMultiplier(1.85f)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("decreaseMultiplier must be between 0 and 1");
    }

    @Test
    public void testIncreaseMultiplierConfig() {
        float increaseMultiplier = 1.85f;
        assertThat(AdaptiveBulkheadConfig.custom()
            .increaseMultiplier(increaseMultiplier)
            .build()
            .getIncreaseMultiplier())
            .isEqualTo(increaseMultiplier)
            .isNotEqualTo(DEFAULT_BULKHEAD.getIncreaseMultiplier());
    }

    @Test
    public void testRecordResultPredicate() {
        Predicate<Object> predicate = result -> true;
        assertThat(AdaptiveBulkheadConfig.custom()
            .recordResult(predicate)
            .build()
            .getRecordResultPredicate())
            .isEqualTo(predicate)
            .isNotEqualTo(DEFAULT_BULKHEAD.getRecordResultPredicate());
    }

    @Test
    public void testResetMetricsOnTransition() {
        boolean resetMetricsOnTransition = true;
        assertThat(AdaptiveBulkheadConfig.custom()
            .resetMetricsOnTransition(resetMetricsOnTransition)
            .build()
            .isResetMetricsOnTransition())
            .isEqualTo(resetMetricsOnTransition)
            .isNotEqualTo(DEFAULT_BULKHEAD.isResetMetricsOnTransition());
    }

    @Test
    public void testInvalidIncreaseMultiplierConfig() {
        assertThatThrownBy(() -> AdaptiveBulkheadConfig.custom()
            .increaseMultiplier(0.85f)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("increaseMultiplier must greater than 1");
    }

    @Test
    public void testSlowCallDurationThresholdConfig() {
        assertThatThrownBy(() -> AdaptiveBulkheadConfig.custom()
            .slowCallDurationThreshold(Duration.ofMillis(0))
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("slowCallDurationThreshold must be at least 1[ns]");
    }

    @Test
    public void testInvalidMaxConcurrentCallsConfig() {
        assertThatThrownBy(() -> AdaptiveBulkheadConfig.custom()
            .maxConcurrentCalls(0)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxConcurrentCalls must greater than 0");
    }

    @Test
    public void testInvalidMinConcurrentCallsConfig() {
        assertThatThrownBy(() -> AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(0)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("minConcurrentCalls must greater than 0");
    }

    @Test
    public void testCurrentTimestampFunctionConfig() {
        Instant instant = Instant.now();
        Clock fixedClock = Clock.fixed(instant, ZoneId.systemDefault());
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .currentTimestampFunction(clock -> clock.instant().toEpochMilli(), TimeUnit.MILLISECONDS)
            .build();

        Function<Clock, Long> currentTimeFunction = config.getCurrentTimestampFunction();

        then(currentTimeFunction).isNotNull();
        then(currentTimeFunction.apply(fixedClock)).isEqualTo(instant.toEpochMilli());
    }

    @Test
    public void testToStringEquality() {
        AdaptiveBulkheadConfig expected = AdaptiveBulkheadConfig.custom()
            .decreaseMultiplier(0.6f)
            .increaseMultiplier(1.6f)
            .minConcurrentCalls(3)
            .maxConcurrentCalls(3)
            .slowCallDurationThreshold(Duration.ofMillis(150))
            .slowCallRateThreshold(50)
            .failureRateThreshold(50)
            .slidingWindowSize(100)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .build();
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .decreaseMultiplier(0.6f)
            .increaseMultiplier(1.6f)
            .minConcurrentCalls(3)
            .maxConcurrentCalls(3)
            .slowCallDurationThreshold(Duration.ofMillis(150))
            .slowCallRateThreshold(50)
            .failureRateThreshold(50)
            .slidingWindowSize(100)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .build();

        assertThat(config.toString())
            .isEqualTo(expected.toString())
            .isEqualTo(AdaptiveBulkheadConfig.from(config)
                .build()
                .toString());
    }

    @Test
    public void shouldUseCustomIgnoreExceptionPredicate() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .ignoreException(e -> "ignore".equals(e.getMessage())).build();
        Predicate<Throwable> ignoreExceptionPredicate = config.getIgnoreExceptionPredicate();
        then(ignoreExceptionPredicate.test(new Error("ignore"))).isEqualTo(true);
        then(ignoreExceptionPredicate.test(new Error("fail"))).isEqualTo(false);
        then(ignoreExceptionPredicate.test(new RuntimeException("ignore"))).isEqualTo(true);
        then(ignoreExceptionPredicate.test(new Error())).isEqualTo(false);
        then(ignoreExceptionPredicate.test(new RuntimeException())).isEqualTo(false);
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

    private static class ExtendsError extends Error {
    }

    @Test
    public void shouldUseIgnoreExceptionsToBuildPredicate() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .ignoreExceptions(RuntimeException.class, ExtendsExtendsException.class, BusinessException.class).build();
        final Predicate<? super Throwable> ignoreExceptionPredicate = config.getIgnoreExceptionPredicate();
        then(ignoreExceptionPredicate.test(new Exception())).isEqualTo(false); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsError())).isEqualTo(false); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsException())).isEqualTo(false); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new BusinessException())).isEqualTo(true); // explicitly ignored
        then(ignoreExceptionPredicate.test(new RuntimeException())).isEqualTo(true); // explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsRuntimeException())).isEqualTo(true); // inherits ignored because of RuntimeException is ignored
        then(ignoreExceptionPredicate.test(new ExtendsExtendsException())).isEqualTo(true); // explicitly ignored
    }

    @Test
    public void shouldUseRecordExceptionsToBuildPredicate() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .recordExceptions(RuntimeException.class, ExtendsExtendsException.class).build();
        final Predicate<? super Throwable> failurePredicate = config
            .getRecordExceptionPredicate();
        then(failurePredicate.test(new Exception())).isFalse(); // not explicitly record
        then(failurePredicate.test(new ExtendsError())).isFalse(); // not explicitly included
        then(failurePredicate.test(new ExtendsException())).isFalse(); // not explicitly included
        then(failurePredicate.test(new BusinessException())).isFalse(); // not explicitly included
        then(failurePredicate.test(new RuntimeException())).isTrue(); // explicitly included
        then(failurePredicate.test(new ExtendsRuntimeException())).isTrue(); // inherits included because RuntimeException is included
        then(failurePredicate.test(new ExtendsExtendsException())).isTrue(); // explicitly included
    }

    @Test
    public void shouldCreateCombinedRecordExceptionPredicate() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .recordException(e -> "test".equals(e.getMessage())) //1
            .recordExceptions(RuntimeException.class, ExtendsExtendsException.class) //2
            .build();
        final Predicate<? super Throwable> recordExceptionPredicate = config.getRecordExceptionPredicate();
        then(recordExceptionPredicate.test(new Exception())).isEqualTo(false); // not explicitly included
        then(recordExceptionPredicate.test(new Exception("test"))).isEqualTo(true); // explicitly included by 1
        then(recordExceptionPredicate.test(new ExtendsError())).isEqualTo(false); // not explicitly included
        then(recordExceptionPredicate.test(new ExtendsException())).isEqualTo(false);  // explicitly excluded by 3
        then(recordExceptionPredicate.test(new ExtendsException("test"))).isEqualTo(true);  // explicitly included by 1
        then(recordExceptionPredicate.test(new BusinessException())).isEqualTo(false); // not explicitly included
        then(recordExceptionPredicate.test(new RuntimeException())).isEqualTo(true); // explicitly included by 2
        then(recordExceptionPredicate.test(new ExtendsRuntimeException())).isEqualTo(true); // implicitly included by RuntimeException
        then(recordExceptionPredicate.test(new ExtendsExtendsException())).isEqualTo(true); // explicitly included
    }

    @Test
    public void shouldCreateCombinedIgnoreExceptionPredicate() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .ignoreException(e -> "ignore".equals(e.getMessage())) //1
            .ignoreExceptions(BusinessException.class, ExtendsExtendsException.class, ExtendsRuntimeException.class) //2
            .build();
        final Predicate<? super Throwable> ignoreExceptionPredicate = config.getIgnoreExceptionPredicate();
        then(ignoreExceptionPredicate.test(new Exception())).isEqualTo(false); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new Exception("ignore"))).isEqualTo(true); // explicitly ignored by 1
        then(ignoreExceptionPredicate.test(new ExtendsError())).isEqualTo(false); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsException())).isEqualTo(false);  // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsException("ignore"))).isEqualTo(true);  // explicitly ignored 1
        then(ignoreExceptionPredicate.test(new BusinessException())).isEqualTo(true); // explicitly ignored 2
        then(ignoreExceptionPredicate.test(new RuntimeException())).isEqualTo(false); // not explicitly ignored
        then(ignoreExceptionPredicate.test(new ExtendsRuntimeException())).isEqualTo(true); // explicitly ignored 2
        then(ignoreExceptionPredicate.test(new ExtendsExtendsException())).isEqualTo(true); // implicitly ignored by ExtendsRuntimeException
    }

}