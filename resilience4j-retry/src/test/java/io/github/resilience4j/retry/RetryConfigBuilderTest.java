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

import org.junit.Test;

import java.time.Duration;
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
        assertThat(config.getIntervalFunction().apply(1)).isEqualTo(0);
    }

    @Test
    public void waitIntervalUnderTenMillisShouldSucceed() {
        RetryConfig config = RetryConfig.custom().waitDuration(Duration.ofMillis(5)).build();
        assertThat(config.getIntervalFunction().apply(1)).isEqualTo(5L);
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
    public void waitIntervalOverTenMillisShouldSucceed() {
        RetryConfig config = RetryConfig.custom().waitDuration(Duration.ofSeconds(10)).build();
        assertThat(config).isNotNull();
    }

    @Test()
    public void shouldUseIgnoreExceptionToBuildPredicate() {
        RetryConfig retryConfig = RetryConfig.custom()
            .ignoreExceptions(RuntimeException.class, ExtendsExtendsException.class).build();
        final Predicate<? super Throwable> failurePredicate = retryConfig.getExceptionPredicate();
        then(failurePredicate.test(new Exception())).isEqualTo(true); // not explicitly excluded
        then(failurePredicate.test(new ExtendsError())).isEqualTo(true); // not explicitly excluded
        then(failurePredicate.test(new ExtendsException()))
            .isEqualTo(true); // not explicitly excluded
        then(failurePredicate.test(new ExtendsException2()))
            .isEqualTo(true); // not explicitly excluded
        then(failurePredicate.test(new RuntimeException())).isEqualTo(false); // explicitly excluded
        then(failurePredicate.test(new ExtendsRuntimeException()))
            .isEqualTo(false); // inherits excluded from ExtendsException
        then(failurePredicate.test(new ExtendsExtendsException()))
            .isEqualTo(false); // explicitly excluded
    }

    @Test()
    public void shouldUseRecordExceptionToBuildPredicate() {
        RetryConfig retryConfig = RetryConfig.custom()
            .retryExceptions(RuntimeException.class, ExtendsExtendsException.class).build();
        final Predicate<? super Throwable> failurePredicate = retryConfig.getExceptionPredicate();
        then(failurePredicate.test(new Exception())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsError())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsException()))
            .isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsException2()))
            .isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new RuntimeException())).isEqualTo(true); // explicitly included
        then(failurePredicate.test(new ExtendsRuntimeException()))
            .isEqualTo(true); // inherits included from ExtendsException
        then(failurePredicate.test(new ExtendsExtendsException()))
            .isEqualTo(true); // explicitly included
    }

    @Test()
    public void shouldUseIgnoreExceptionOverRecordToBuildPredicate() {
        RetryConfig retryConfig = RetryConfig.custom()
            .retryExceptions(RuntimeException.class, ExtendsExtendsException.class)
            .ignoreExceptions(ExtendsException.class, ExtendsRuntimeException.class)
            .build();
        final Predicate<? super Throwable> failurePredicate = retryConfig.getExceptionPredicate();
        then(failurePredicate.test(new Exception())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsError())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new ExtendsException()))
            .isEqualTo(false);  // explicitly excluded
        then(failurePredicate.test(new ExtendsException2()))
            .isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new RuntimeException())).isEqualTo(true); // explicitly included
        then(failurePredicate.test(new ExtendsRuntimeException()))
            .isEqualTo(false); // explicitly excluded
        then(failurePredicate.test(new ExtendsExtendsException()))
            .isEqualTo(false); // inherits excluded from ExtendsException
    }

    @Test()
    public void shouldUseBothRecordToBuildPredicate() {
        RetryConfig retryConfig = RetryConfig.custom()
            .retryOnException(TEST_PREDICATE) //1
            .retryExceptions(RuntimeException.class, ExtendsExtendsException.class) //2
            .ignoreExceptions(ExtendsException.class, ExtendsRuntimeException.class) //3
            .build();
        final Predicate<? super Throwable> failurePredicate = retryConfig.getExceptionPredicate();
        then(failurePredicate.test(new Exception())).isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new Exception("test")))
            .isEqualTo(true); // explicitly included by 1
        then(failurePredicate.test(new ExtendsError())).isEqualTo(false); // ot explicitly included
        then(failurePredicate.test(new ExtendsException()))
            .isEqualTo(false);  // explicitly excluded by 3
        then(failurePredicate.test(new ExtendsException("test")))
            .isEqualTo(false);  // explicitly excluded by 3 even if included by 1
        then(failurePredicate.test(new ExtendsException2()))
            .isEqualTo(false); // not explicitly included
        then(failurePredicate.test(new RuntimeException()))
            .isEqualTo(true); // explicitly included by 2
        then(failurePredicate.test(new ExtendsRuntimeException()))
            .isEqualTo(false); // explicitly excluded by 3
        then(failurePredicate.test(new ExtendsExtendsException()))
            .isEqualTo(false); // inherits excluded from ExtendsException by 3
    }

    @Test()
    public void shouldBuilderCreateConfigEveryTime() {
        final RetryConfig.Builder builder = RetryConfig.custom();
        builder.maxAttempts(5);
        final RetryConfig config1 = builder.build();
        builder.maxAttempts(3);
        final RetryConfig config2 = builder.build();
        assertThat(config2.getMaxAttempts()).isEqualTo(3);
        assertThat(config1.getMaxAttempts()).isEqualTo(5);
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
