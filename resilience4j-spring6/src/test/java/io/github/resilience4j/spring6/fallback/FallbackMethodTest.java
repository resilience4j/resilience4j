/*
 * Copyright 2019 Kyuhyen Hwang , Mahmoud Romih
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.spring6.fallback;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("unused")
public class FallbackMethodTest {

    @Test
    public void fallbackRuntimeExceptionTest() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod fallbackMethod = FallbackMethod
            .create("fallbackMethod", testMethod, new Object[]{"test"}, target.getClass(), target);
        assertThat(fallbackMethod.fallback(new RuntimeException("err")))
            .isEqualTo("recovered-RuntimeException");
    }

    @Test
    public void fallbackFuture() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testFutureMethod", String.class);
        FallbackMethod fallbackMethod = FallbackMethod
            .create("futureFallbackMethod", testMethod, new Object[]{"test"}, target.getClass(), target);
        CompletableFuture future = (CompletableFuture) fallbackMethod.fallback(new IllegalStateException("err"));
        assertThat(future.get()).isEqualTo("recovered-IllegalStateException");
    }

    @Test
    public void fallbackGlobalExceptionWithSameMethodReturnType() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod fallbackMethod = FallbackMethod
            .create("fallbackMethod", testMethod, new Object[]{"test"}, target.getClass(), target);
        assertThat(fallbackMethod.fallback(new IllegalStateException("err")))
            .isEqualTo("recovered-IllegalStateException");
    }

    @Test
    public void fallbackGlobalExceptionWithSameMethodReturnTypeAndMultipleParameters() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("multipleParameterTestMethod", String.class, String.class);

        FallbackMethod fallbackMethod = FallbackMethod
            .create("fallbackMethod", testMethod, new Object[]{"test", "test"}, target.getClass(), target);

        assertThat(fallbackMethod.fallback(new IllegalStateException("err")))
            .isEqualTo("recovered-IllegalStateException");
    }

    @Test
    public void fallbackClosestSuperclassExceptionTest() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod fallbackMethod = FallbackMethod
            .create("fallbackMethod", testMethod, new Object[]{"test"}, target.getClass(), target);
        assertThat(fallbackMethod.fallback(new NumberFormatException("err")))
            .isEqualTo("recovered-IllegalArgumentException");
    }

    @Test
    public void shouldThrowUnrecoverableThrowable() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod fallbackMethod = FallbackMethod
            .create("fallbackMethod", testMethod, new Object[]{"test"}, target.getClass(), target);
        Throwable unrecoverableThrown = new Throwable("err");
        assertThatThrownBy(() -> fallbackMethod.fallback(unrecoverableThrown))
            .isEqualTo(unrecoverableThrown);
    }

    @Test
    public void shouldCallPrivateFallbackMethod() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod fallbackMethod = FallbackMethod
            .create("privateFallback", testMethod, new Object[]{"test"}, target.getClass(), target);
        assertThat(fallbackMethod.fallback(new RuntimeException("err")))
            .isEqualTo("recovered-privateMethod");
    }

    @Test
    public void mismatchReturnType_shouldThrowNoSuchMethodException() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        assertThatThrownBy(() -> FallbackMethod
            .create("duplicateException", testMethod, new Object[]{"test"}, target.getClass(), target))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "You have more that one fallback method that cover the same exception type java.lang.IllegalArgumentException");
    }

    @Test
    public void shouldFailIf2FallBackMethodsHandleSameException() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        assertThatThrownBy(() -> FallbackMethod
            .create("returnMismatchFallback", testMethod, new Object[]{"test"}, target.getClass(), target))
            .isInstanceOf(NoSuchMethodException.class)
            .hasMessage(
                "class java.lang.String class io.github.resilience4j.spring6.fallback.FallbackMethodTest.returnMismatchFallback(class java.lang.String,class java.lang.Throwable)");
    }

    @Test
    public void notFoundFallbackMethod_shouldThrowsNoSuchMethodException() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        assertThatThrownBy(
            () -> FallbackMethod.create("noMethod", testMethod, new Object[]{"test"}, target.getClass(), target))
            .isInstanceOf(NoSuchMethodException.class)
            .hasMessage(
                "class java.lang.String class io.github.resilience4j.spring6.fallback.FallbackMethodTest.noMethod(class java.lang.String,class java.lang.Throwable)");
    }

    @Test
    public void rethrownFallbackMethodRuntimeExceptionShouldNotBeWrapped() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod fallbackMethod = FallbackMethod
            .create("rethrowingFallbackMethod", testMethod, new Object[]{"test"}, target.getClass(), target);
        RethrowException exception = new RethrowException();
        assertThatThrownBy(() -> fallbackMethod.fallback(exception)).isSameAs(exception);
    }

    @Test
    public void rethrownFallbackMethodCheckedExceptionShouldNotBeWrapped() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod fallbackMethod = FallbackMethod
            .create("rethrowingFallbackMethodChecked", testMethod, new Object[]{"test"}, target.getClass(), target);
        RethrowCheckedException exception = new RethrowCheckedException();
        assertThatThrownBy(() -> fallbackMethod.fallback(exception)).isSameAs(exception);
    }

    public String testMethod(String parameter) {
        return "test";
    }

    public String multipleParameterTestMethod(String param1, String param2) {
        return "multiple parameter test";
    }

    public CompletableFuture<String> testFutureMethod(String parameter) {
        return CompletableFuture.completedFuture("test");
    }

    public String fallbackMethod(String parameter, RuntimeException exception) {
        return "recovered-RuntimeException";
    }

    public String fallbackMethod(IllegalStateException exception) {
        return "recovered-IllegalStateException";
    }

    public CompletableFuture<String> futureFallbackMethod(String parameter, IllegalStateException exception) {
        return CompletableFuture.completedFuture("recovered-IllegalStateException");
    }

    public String fallbackMethod(String parameter, IllegalArgumentException exception) {
        return "recovered-IllegalArgumentException";
    }

    public Object returnMismatchFallback(String parameter, RuntimeException exception) {
        return "recovered";
    }

    private String privateFallback(String parameter, RuntimeException exception) {
        return "recovered-privateMethod";
    }

    public String duplicateException(String parameter, IllegalArgumentException exception) {
        return "recovered-IllegalArgumentException";
    }

    public String duplicateException(IllegalArgumentException exception) {
        return "recovered-IllegalArgumentException";
    }

    public String rethrowingFallbackMethod(String parameter, Exception exception) {
        // To illustrate the typical use case:
        if (exception instanceof RethrowException) {
            throw (RethrowException) exception;
        }
        return "normal recovery result";
    }

    public String rethrowingFallbackMethodChecked(String parameter, Exception exception)
        throws Exception {
        throw exception;
    }
}
