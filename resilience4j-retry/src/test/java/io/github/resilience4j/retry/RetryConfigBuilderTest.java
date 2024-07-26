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
package io.github.resilience4j.retry;

import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.core.IntervalFunction;
import org.junit.Test;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

public class RetryConfigBuilderTest {

    private static final Predicate<Throwable> TEST_PREDICATE = e -> "test".equals(e.getMessage());

    @Test(expected = IllegalArgumentException.class)
    public void zeroMaxAttemptsShouldFail() {
        RetryConfig.custom().maxAttempts(0).build();
    }

    @Test
    public void zeroWaitInterval() {
        final RetryConfig config = RetryConfig.custom().waitDuration(Duration.ofMillis(0)).build();
        assertThat(config.getIntervalBiFunction().apply(1, null)).isZero();
    }

    @Test
    public void waitIntervalUnderTenMillisShouldSucceed() {
        RetryConfig config = RetryConfig.custom().waitDuration(Duration.ofMillis(5)).build();
        assertThat(config.getIntervalBiFunction().apply(1, null)).isEqualTo(5L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void waitIntervalUnderZeroShouldFail() {
        RetryConfig.custom().waitDuration(Duration.ofMillis(-1)).build();
    }

    @Test
    public void waitIntervalOfTenMillisShouldSucceed() {
        RetryConfig config = RetryConfig.custom().waitDuration(Duration.ofMillis(10)).build();
        assertThat(config).isNotNull();
    }

    @Test
    public void testCreateFromConfigurationWithNoPredicateCalculations() {
        RetryConfig config = RetryConfig
            .from(RetryConfig.custom().retryOnException(e -> e instanceof IllegalArgumentException)
                .build()).build();
        assertThat(config).isNotNull();
        assertThat(config.getExceptionPredicate().test(new IllegalArgumentException())).isTrue();
        assertThat(config.getExceptionPredicate().test(new IllegalStateException())).isFalse();
    }

    @Test
    public void testCreateFromConfigurationShouldCopyIntervalBiFunction() {
        IntervalBiFunction<Object> biFunction = IntervalBiFunction.ofIntervalFunction(IntervalFunction.ofDefaults());
        RetryConfig config = RetryConfig
            .from(RetryConfig.custom()
                .intervalBiFunction(biFunction)
                .build()).build();
        assertThat(config).isNotNull();
        assertThat(config.getIntervalBiFunction()).isNotNull();
        assertThat(config.getIntervalBiFunction()).isEqualTo(biFunction);
    }

    @Test
    public void testCreateFromConfigurationShouldUseIntervalFunction() {
        RetryConfig config = RetryConfig
            .from(RetryConfig.custom()
                .intervalFunction(IntervalFunction.of(100L))
                .build()).build();
        assertThat(config).isNotNull();
        assertThat(config.getIntervalBiFunction()).isNotNull();
        assertThat(config.getIntervalBiFunction().apply(1, null)).isEqualTo(100L);
    }

    @Test
    public void testCreateFromDefaultConfigurationShouldUseIntervalFunction() {
        RetryConfig baseConfig = RetryConfig.ofDefaults();
        RetryConfig retryConfig = RetryConfig.from(baseConfig)
            .intervalFunction(IntervalFunction.of(100L))
            .build();

        assertThat(retryConfig).isNotNull();
        assertThat(retryConfig.getIntervalBiFunction()).isNotNull();
        assertThat(retryConfig.getIntervalFunction().apply(1)).isEqualTo(100L);
    }

    @Test
    public void testConsumeResultBeforeRetryAttemptCanBeConfigured(){
        BiConsumer<Integer, String> biConsumer = (attempt, resultObject) -> {};
        RetryConfig.Builder<String> builder = RetryConfig.custom();
        RetryConfig retryConfig = builder.consumeResultBeforeRetryAttempt(biConsumer).build();

        assertThat(retryConfig).isNotNull();
        assertThat(retryConfig.getConsumeResultBeforeRetryAttempt()).isNotNull();
        assertThat(retryConfig.getConsumeResultBeforeRetryAttempt()).isEqualTo(biConsumer);
    }

    @Test
    public void waitIntervalOverTenMillisShouldSucceed() {
        RetryConfig config = RetryConfig.custom().waitDuration(Duration.ofSeconds(10)).build();
        assertThat(config).isNotNull();
    }

    @Test()
    public void shouldUseIgnoreExceptionToBuildPredicate() {
        RetryConfig retryConfig = RetryConfig.custom()
            .ignoreExceptions(RuntimeException.class, ExtendsExtendsException.class).build();
        final Predicate<? super Throwable> failurePredicate = retryConfig.getExceptionPredicate();
        then(failurePredicate.test(new Exception())).isTrue(); // not explicitly excluded
        then(failurePredicate.test(new ExtendsError())).isTrue(); // not explicitly excluded
        then(failurePredicate.test(new ExtendsException()))
            .isTrue(); // not explicitly excluded
        then(failurePredicate.test(new ExtendsException2()))
            .isTrue(); // not explicitly excluded
        then(failurePredicate.test(new RuntimeException())).isFalse(); // explicitly excluded
        then(failurePredicate.test(new ExtendsRuntimeException()))
            .isFalse(); // inherits excluded from ExtendsException
        then(failurePredicate.test(new ExtendsExtendsException()))
            .isFalse(); // explicitly excluded
    }

    @Test()
    public void shouldUseRecordExceptionToBuildPredicate() {
        RetryConfig retryConfig = RetryConfig.custom()
            .retryExceptions(RuntimeException.class, ExtendsExtendsException.class).build();
        final Predicate<? super Throwable> failurePredicate = retryConfig.getExceptionPredicate();
        then(failurePredicate.test(new Exception())).isFalse(); // not explicitly included
        then(failurePredicate.test(new ExtendsError())).isFalse(); // not explicitly included
        then(failurePredicate.test(new ExtendsException()))
            .isFalse(); // not explicitly included
        then(failurePredicate.test(new ExtendsException2()))
            .isFalse(); // not explicitly included
        then(failurePredicate.test(new RuntimeException())).isTrue(); // explicitly included
        then(failurePredicate.test(new ExtendsRuntimeException()))
            .isTrue(); // inherits included from ExtendsException
        then(failurePredicate.test(new ExtendsExtendsException()))
            .isTrue(); // explicitly included
    }

    @Test()
    public void shouldUseIgnoreExceptionOverRecordToBuildPredicate() {
        RetryConfig retryConfig = RetryConfig.custom()
            .retryExceptions(RuntimeException.class, ExtendsExtendsException.class)
            .ignoreExceptions(ExtendsException.class, ExtendsRuntimeException.class)
            .build();
        final Predicate<? super Throwable> failurePredicate = retryConfig.getExceptionPredicate();
        then(failurePredicate.test(new Exception())).isFalse(); // not explicitly included
        then(failurePredicate.test(new ExtendsError())).isFalse(); // not explicitly included
        then(failurePredicate.test(new ExtendsException()))
            .isFalse();  // explicitly excluded
        then(failurePredicate.test(new ExtendsException2()))
            .isFalse(); // not explicitly included
        then(failurePredicate.test(new RuntimeException())).isTrue(); // explicitly included
        then(failurePredicate.test(new ExtendsRuntimeException()))
            .isFalse(); // explicitly excluded
        then(failurePredicate.test(new ExtendsExtendsException()))
            .isFalse(); // inherits excluded from ExtendsException
    }

    @Test()
    public void shouldUseBothRecordToBuildPredicate() {
        RetryConfig retryConfig = RetryConfig.custom()
            .retryOnException(TEST_PREDICATE) //1
            .retryExceptions(RuntimeException.class, ExtendsExtendsException.class) //2
            .ignoreExceptions(ExtendsException.class, ExtendsRuntimeException.class) //3
            .build();
        final Predicate<? super Throwable> failurePredicate = retryConfig.getExceptionPredicate();
        then(failurePredicate.test(new Exception())).isFalse(); // not explicitly included
        then(failurePredicate.test(new Exception("test")))
            .isTrue(); // explicitly included by 1
        then(failurePredicate.test(new ExtendsError())).isFalse(); // ot explicitly included
        then(failurePredicate.test(new ExtendsException()))
            .isFalse();  // explicitly excluded by 3
        then(failurePredicate.test(new ExtendsException("test")))
            .isFalse();  // explicitly excluded by 3 even if included by 1
        then(failurePredicate.test(new ExtendsException2()))
            .isFalse(); // not explicitly included
        then(failurePredicate.test(new RuntimeException()))
            .isTrue(); // explicitly included by 2
        then(failurePredicate.test(new ExtendsRuntimeException()))
            .isFalse(); // explicitly excluded by 3
        then(failurePredicate.test(new ExtendsExtendsException()))
            .isFalse(); // inherits excluded from ExtendsException by 3
    }

    @Test()
    public void shouldBuilderCreateConfigEveryTime() {
        final RetryConfig.Builder<Object> builder = RetryConfig.custom();
        builder.maxAttempts(5);
        final RetryConfig config1 = builder.build();
        builder.maxAttempts(3);
        final RetryConfig config2 = builder.build();
        assertThat(config2.getMaxAttempts()).isEqualTo(3);
        assertThat(config1.getMaxAttempts()).isEqualTo(5);
    }

    @Test()
    public void intervalFunctionClearIntervalBiFunction() {
        IntervalBiFunction<Object> biFunction = (attempt, either) -> 100L;
        IntervalFunction function = IntervalFunction.ofDefaults();
        RetryConfig config = RetryConfig.custom().intervalBiFunction(biFunction)
                .intervalFunction(function)
                .build();
        assertThat(config).isNotNull();
        assertThat(config.getIntervalFunction()).isEqualTo(function);
        assertThat(config.getIntervalBiFunction()).isNotEqualTo(biFunction);
    }

    @Test
    public void intervalBiFunctionClearIntervalFunction() {
        IntervalBiFunction<Object> biFunction = (attempt, either) -> 100L;
        IntervalFunction function = IntervalFunction.ofDefaults();
        RetryConfig config = RetryConfig.custom().intervalFunction(function)
                .intervalBiFunction(biFunction)
                .build();
        assertThat(config).isNotNull();
        assertThat(config.getIntervalFunction()).isNotNull();
        assertThat(config.getIntervalFunction()).isNotEqualTo(function);
        assertThat(config.getIntervalBiFunction()).isEqualTo(biFunction);
    }

    @Test
    public void shouldUseDefaultIntervalFunction() {
        RetryConfig retryConfig = RetryConfig.ofDefaults();

        assertThat(retryConfig.getIntervalFunction()).isNotNull();
        assertThat(retryConfig.getIntervalBiFunction()).isNotNull();
        assertThat(retryConfig.getIntervalBiFunction().apply(1, null)).isEqualTo(IntervalFunction.ofDefaults().apply(1));
    }

    @Test
    public void shouldUseSetIntervalFunction() {
        RetryConfig retryConfig = RetryConfig.custom()
            .intervalFunction(IntervalFunction.of(100L))
            .build();

        assertThat(retryConfig.getIntervalFunction()).isNotNull();
        assertThat(retryConfig.getIntervalBiFunction()).isNotNull();
        assertThat(retryConfig.getIntervalBiFunction().apply(1, null)).isEqualTo(100L);
    }

    @Test
    public void shouldUseSetIntervalBiFunction() {
        RetryConfig retryConfig = RetryConfig.custom()
            .intervalBiFunction(IntervalBiFunction.ofIntervalFunction(IntervalFunction.of(105L)))
            .build();

        assertThat(retryConfig.getIntervalFunction()).isNull();
        assertThat(retryConfig.getIntervalBiFunction()).isNotNull();
        assertThat(retryConfig.getIntervalBiFunction().apply(1, null)).isEqualTo(105L);
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

    private static class ExtendsException2 extends Exception {

    }

    private static class ExtendsError extends Error {

    }

}
