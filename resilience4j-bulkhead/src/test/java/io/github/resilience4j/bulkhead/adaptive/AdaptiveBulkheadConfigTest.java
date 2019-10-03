package io.github.resilience4j.bulkhead.adaptive;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.function.Predicate;

import org.junit.Test;

import io.github.resilience4j.bulkhead.adaptive.internal.config.AbstractConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AimdConfig;


/**
 *
 */
public class AdaptiveBulkheadConfigTest {
	@Test
	public void testBuildCustom() {
		AdaptiveBulkheadConfig<AimdConfig> config = AdaptiveBulkheadConfig.<AimdConfig>builder()
				.config(AimdConfig.builder()
						.concurrencyDropMultiplier(0.3)
						.minConcurrentRequestsLimit(3)
						.maxConcurrentRequestsLimit(3)
						.slowCallDurationThreshold(150)
						.slowCallRateThreshold(50)
						.failureRateThreshold(50)
						.slidingWindowTime(5)
						.slidingWindowSize(100)
						.slidingWindowType(AbstractConfig.SlidingWindow.TIME_BASED)
						.build()).build();

		assertThat(config).isNotNull();
		assertThat(config.getConfiguration().getConcurrencyDropMultiplier()).isEqualTo(0.85);
		assertThat(config.getConfiguration().getMinLimit()).isEqualTo(3);
		assertThat(config.getConfiguration().getMaxLimit()).isEqualTo(3);
		assertThat(config.getConfiguration().getDesirableLatency().toMillis()).isEqualTo(150);
		assertThat(config.getConfiguration().getSlidingWindowSize()).isEqualTo(100);
		assertThat(config.getConfiguration().getSlidingWindowTime()).isEqualTo(5);
		assertThat(config.getConfiguration().getSlowCallRateThreshold()).isEqualTo(50);
		assertThat(config.getConfiguration().getFailureRateThreshold()).isEqualTo(50);
		assertThat(config.getConfiguration().getSlidingWindowType()).isEqualTo(AbstractConfig.SlidingWindow.TIME_BASED);

	}


	@Test
	public void tesConcurrencyDropMultiplierConfig() {
		final AimdConfig build = AimdConfig.builder().concurrencyDropMultiplier(0.0).build();
		assertThat(build.getConcurrencyDropMultiplier()).isEqualTo(0.85d);
	}

	@Test
	public void testNotSetDesirableOperationLatencyConfig() {
		assertThatThrownBy(() -> AimdConfig.builder().slowCallDurationThreshold(0).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("slowCallDurationThreshold must be a positive value greater than zero");
	}

	@Test
	public void testNotSetMaxAcceptableRequestLatencyConfig() {
		assertThatThrownBy(() -> AimdConfig.builder().maxConcurrentRequestsLimit(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("maxConcurrentRequestsLimit must be a positive value greater than zero");
	}

	@Test
	public void testNotSetMinAcceptableRequestLatencyConfig() {
		assertThatThrownBy(() -> AimdConfig.builder().minConcurrentRequestsLimit(0)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("minConcurrentRequestsLimit must be a positive value greater than zero");
	}


	@Test
	public void testLimitIncrementInflightFactor() {
		final AimdConfig build = AimdConfig.builder().limitIncrementInflightFactor(1).build();
		assertThat(build.getLimitIncrementInflightFactor()).isEqualTo(1);
	}


	@Test
	public void testEqual() {
		AimdConfig config = AimdConfig.builder()
				.concurrencyDropMultiplier(0.6)
				.minConcurrentRequestsLimit(3)
				.maxConcurrentRequestsLimit(3)
				.slowCallDurationThreshold(150)
				.slowCallRateThreshold(50)
				.failureRateThreshold(50)
				.slidingWindowTime(5)
				.slidingWindowSize(100)
				.slidingWindowType(AbstractConfig.SlidingWindow.TIME_BASED)
				.build();
		AimdConfig config2 = AimdConfig.builder()
				.concurrencyDropMultiplier(0.6)
				.minConcurrentRequestsLimit(3)
				.maxConcurrentRequestsLimit(3)
				.slowCallDurationThreshold(150)
				.slowCallRateThreshold(50)
				.failureRateThreshold(50)
				.slidingWindowTime(5)
				.slidingWindowSize(100)
				.slidingWindowType(AbstractConfig.SlidingWindow.TIME_BASED)
				.build();

		final AimdConfig build = AimdConfig.from(config2).build();
		assertThat(build.equals(config2));
		assertThat(AimdConfig.ofDefaults().getMaxLimit()).isEqualTo(200);
		assertThat(config.equals(config2)).isTrue();
		assertThat(config.hashCode() == config2.hashCode()).isTrue();
	}


	@Test
	public void shouldUseCustomIgnoreExceptionPredicate() {
		AdaptiveBulkheadConfig<AimdConfig> config = AdaptiveBulkheadConfig.<AimdConfig>builder().config(AimdConfig.ofDefaults())
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
		AdaptiveBulkheadConfig<AimdConfig> config = AdaptiveBulkheadConfig.<AimdConfig>builder().config(AimdConfig.ofDefaults())
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
		AdaptiveBulkheadConfig<AimdConfig> config = AdaptiveBulkheadConfig.<AimdConfig>builder().config(AimdConfig.ofDefaults())
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
		AdaptiveBulkheadConfig<AimdConfig> config = AdaptiveBulkheadConfig.<AimdConfig>builder().config(AimdConfig.ofDefaults())
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
		AdaptiveBulkheadConfig<AimdConfig> config = AdaptiveBulkheadConfig.<AimdConfig>builder().config(AimdConfig.ofDefaults())
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