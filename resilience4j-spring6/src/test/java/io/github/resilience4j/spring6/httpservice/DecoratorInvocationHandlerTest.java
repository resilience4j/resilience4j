/*
 * Copyright 2026 Bobae Kim
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
package io.github.resilience4j.spring6.httpservice;

import io.github.resilience4j.spring6.httpservice.test.TestHttpService;
import io.github.resilience4j.spring6.httpservice.test.TestHttpServiceDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DecoratorInvocationHandlerTest {

    private DecoratorInvocationHandler testSubject;
    private TestHttpService delegate;
    private Method greetingMethod;
    private TestHttpServiceDecorator decorator;
    private HttpServiceTarget<TestHttpService> target;

    @BeforeEach
    void setUp() throws Exception {
        target = HttpServiceTarget.of(TestHttpService.class, "TestHttpService");
        delegate = mock(TestHttpService.class);
        when(delegate.greeting()).thenReturn("Hello from delegate");

        greetingMethod = TestHttpService.class.getMethod("greeting");
        decorator = new TestHttpServiceDecorator();

        testSubject = new DecoratorInvocationHandler(target, delegate, decorator);
    }

    @Test
    void testInvoke() throws Throwable {
        Object result = testSubject.invoke(delegate, greetingMethod, new Object[0]);

        verify(delegate, times(1)).greeting();
        assertThat(decorator.isCalled())
                .describedAs("HttpServiceDecorator is called")
                .isTrue();
        assertThat(result)
                .describedAs("Return of invocation")
                .isEqualTo("Hello from delegate");
    }

    @Test
    void testDecorator() throws Throwable {
        decorator.setAlternativeFunction(fnArgs -> "AlternativeFunction");
        testSubject = new DecoratorInvocationHandler(target, delegate, decorator);

        Object result = testSubject.invoke(delegate, greetingMethod, new Object[0]);

        verify(delegate, times(0)).greeting();
        assertThat(decorator.isCalled())
                .describedAs("HttpServiceDecorator is called")
                .isTrue();
        assertThat(result)
                .describedAs("Return of invocation")
                .isEqualTo("AlternativeFunction");
    }

    @Test
    void testInvokeToString() throws Throwable {
        Method toStringMethod = Object.class.getMethod("toString");

        Object result = testSubject.invoke(delegate, toStringMethod, new Object[0]);

        verify(delegate, times(0)).greeting();
        assertThat(result)
                .describedAs("Return of invocation")
                .isEqualTo(target.toString());
    }

    @Test
    void testInvokeEquals() throws Throwable {
        Method equalsMethod = Object.class.getMethod("equals", Object.class);

        // Create a proxy to test equals with
        Object proxy = Proxy.newProxyInstance(
                TestHttpService.class.getClassLoader(),
                new Class<?>[]{TestHttpService.class},
                testSubject
        );

        Boolean result = (Boolean) testSubject.invoke(proxy, equalsMethod, new Object[]{proxy});

        verify(delegate, times(0)).greeting();
        assertThat(result)
                .describedAs("Return of invocation")
                .isTrue();
    }

    @Test
    void testInvokeEqualsWithNull() throws Throwable {
        Method equalsMethod = Object.class.getMethod("equals", Object.class);

        Boolean result = (Boolean) testSubject.invoke(delegate, equalsMethod, new Object[]{null});

        assertThat(result)
                .describedAs("Return of invocation")
                .isFalse();
    }

    @Test
    void testInvokeEqualsWithDifferentHandler() throws Throwable {
        Method equalsMethod = Object.class.getMethod("equals", Object.class);

        HttpServiceTarget<TestHttpService> otherTarget = HttpServiceTarget.of(TestHttpService.class, "OtherService");
        DecoratorInvocationHandler otherHandler = new DecoratorInvocationHandler(otherTarget, delegate, decorator);

        Object proxy = Proxy.newProxyInstance(
                TestHttpService.class.getClassLoader(),
                new Class<?>[]{TestHttpService.class},
                testSubject
        );

        Object otherProxy = Proxy.newProxyInstance(
                TestHttpService.class.getClassLoader(),
                new Class<?>[]{TestHttpService.class},
                otherHandler
        );

        Boolean result = (Boolean) testSubject.invoke(proxy, equalsMethod, new Object[]{otherProxy});

        assertThat(result)
                .describedAs("Return of invocation - different targets should not be equal")
                .isFalse();
    }

    @Test
    void testInvokeHashcode() throws Throwable {
        Method hashCodeMethod = Object.class.getMethod("hashCode");

        Integer result = (Integer) testSubject.invoke(delegate, hashCodeMethod, new Object[0]);

        verify(delegate, times(0)).greeting();
        assertThat(result)
                .describedAs("Return of invocation")
                .isEqualTo(target.hashCode());
    }

    @Test
    void testInvokeWithParameters() throws Throwable {
        Method greetingWithNameMethod = TestHttpService.class.getMethod("greetingWithName", String.class);
        when(delegate.greetingWithName("John")).thenReturn("Hello John");

        Object result = testSubject.invoke(delegate, greetingWithNameMethod, new Object[]{"John"});

        verify(delegate, times(1)).greetingWithName("John");
        assertThat(decorator.isCalled()).isTrue();
        assertThat(result).isEqualTo("Hello John");
    }

    @Test
    void testDecoratorThrowsException() {
        decorator.setAlternativeFunction(fnArgs -> {
            throw new RuntimeException("Decorator exception");
        });
        testSubject = new DecoratorInvocationHandler(target, delegate, decorator);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> testSubject.invoke(delegate, greetingMethod, new Object[0]));
        assertThat(e.getMessage()).isEqualTo("Decorator exception");
        verify(delegate, times(0)).greeting();
        assertThat(decorator.isCalled()).isTrue();
    }

    @Test
    void testDelegateThrowsException() {
        when(delegate.greeting()).thenThrow(new RuntimeException("Delegate exception"));

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> testSubject.invoke(delegate, greetingMethod, new Object[0]));
        assertThat(e.getMessage()).isEqualTo("Delegate exception");
        verify(delegate, times(1)).greeting();
        assertThat(decorator.isCalled()).isTrue();
    }
}
