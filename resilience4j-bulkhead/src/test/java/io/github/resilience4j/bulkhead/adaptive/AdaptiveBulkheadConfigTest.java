package io.github.resilience4j.bulkhead.adaptive;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.time.Duration;
import java.util.function.Predicate;

import org.junit.Ignore;
import org.junit.Test;

public class AdaptiveBulkheadConfigTest {
	
    @Test
    public void testBuildCustom() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .decreaseMultiplier(0.3f)
            .minConcurrentCalls(3)
            .maxConcurrentCalls(3)
            .slowCallDurationThreshold(Duration.ofMillis(150))
            .slowCallRateThreshold(50)
            .failureRateThreshold(50)
            .maxWaitDuration(Duration.ofMillis(150))
//						.slidingWindowTime(5)
            .slidingWindowSize(100)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .build();

		assertThat(config).isNotNull();
		assertThat(config.getDecreaseMultiplier()).isEqualTo(0.3f);
        assertThat(config.getMinConcurrentCalls()).isEqualTo(3);
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(config.getMaxWaitDuration().toMillis()).isEqualTo(150);
		assertThat(config.getSlidingWindowSize()).isEqualTo(100);
//		assertThat(config.getSlidingWindowTime()).isEqualTo(5);
		assertThat(config.getSlowCallRateThreshold()).isEqualTo(50);
		assertThat(config.getFailureRateThreshold()).isEqualTo(50);
		assertThat(config.getSlidingWindowType()).isEqualTo(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED);
	}

    @Test
    public void tesConcurrencyDropMultiplierConfig() {
        AdaptiveBulkheadConfig build = AdaptiveBulkheadConfig.custom()
            .decreaseMultiplier(0.85f)
            .build();
        assertThat(build.getDecreaseMultiplier()).isEqualTo(0.85f);
    }

    @Test
    public void testNotSetDesirableOperationLatencyConfig() {
        assertThatThrownBy(() -> AdaptiveBulkheadConfig.custom()
            .slowCallDurationThreshold(Duration.ofMillis(0))
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("slowCallDurationThreshold must be at least 1[ns]");
    }

    @Test
    public void testNotSetMaxAcceptableRequestLatencyConfig() {
        assertThatThrownBy(() -> AdaptiveBulkheadConfig.custom()
            .maxConcurrentCalls(0)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxConcurrentCalls must greater than 0");
    }

    @Test
    public void testNotSetMinAcceptableRequestLatencyConfig() {
        assertThatThrownBy(() -> AdaptiveBulkheadConfig.custom()
            .minConcurrentCalls(0)
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("minConcurrentCalls must greater than 0");
    }

    @Ignore
    @Test
    public void testEqual() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .decreaseMultiplier(0.6f)
            .minConcurrentCalls(3)
            .maxConcurrentCalls(3)
            .slowCallDurationThreshold(Duration.ofMillis(150))
            .slowCallRateThreshold(50)
            .failureRateThreshold(50)
//				.slidingWindowTime(5)
            .slidingWindowSize(100)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .build();
        AdaptiveBulkheadConfig config2 = AdaptiveBulkheadConfig.custom()
            .decreaseMultiplier(0.6f)
            .minConcurrentCalls(3)
            .maxConcurrentCalls(3)
            .slowCallDurationThreshold(Duration.ofMillis(150))
            .slowCallRateThreshold(50)
            .failureRateThreshold(50)
//				.slidingWindowTime(5)
            .slidingWindowSize(100)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .build();

        assertThat(AdaptiveBulkheadConfig.from(config2).build()).isEqualTo(config2);
        assertThat(AdaptiveBulkheadConfig.ofDefaults().getMaxConcurrentCalls()).isEqualTo(200);
		assertThat(config).isEqualTo(config2);
		assertThat(config.hashCode()).isEqualTo(config2.hashCode());
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
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.builder(AdaptiveBulkheadConfig.ofDefaults())
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

	@Ignore
	@Test
	public void shouldUseRecordExceptionsToBuildPredicate() {
		AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.builder(AdaptiveBulkheadConfig.ofDefaults())
				.recordExceptions(RuntimeException.class, ExtendsExtendsException.class).build();
		final Predicate<? super Throwable> failurePredicate = config.getRecordExceptionPredicate();
		then(failurePredicate.test(new Exception())).isEqualTo(false); // not explicitly recore
		then(failurePredicate.test(new ExtendsError())).isEqualTo(false); // not explicitly included
		then(failurePredicate.test(new ExtendsException())).isEqualTo(false); // not explicitly included
		then(failurePredicate.test(new BusinessException())).isEqualTo(false); // not explicitly included
		then(failurePredicate.test(new RuntimeException())).isEqualTo(true); // explicitly included
		then(failurePredicate.test(new ExtendsRuntimeException())).isEqualTo(true); // inherits included because RuntimeException is included
		then(failurePredicate.test(new ExtendsExtendsException())).isEqualTo(true); // explicitly included
	}

	@Test
	public void shouldCreateCombinedRecordExceptionPredicate() {
		AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.builder(AdaptiveBulkheadConfig.ofDefaults())
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
		AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.builder(AdaptiveBulkheadConfig.ofDefaults())
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