/*
 *
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import feign.Target.HardCodedTarget;

public class DecoratorInvocationHandlerTest {

    private DecoratorInvocationHandler testSubject;
    private TestService testService;
    private Method method;
    private TestFeignDecorator feignDecorator;
    private MethodHandler methodHandler;
    private Map<Method, MethodHandler> dispatch;
    private Target<TestService> target;

    @Before
    public void setUp() throws Throwable {
        target = new HardCodedTarget<TestService>(TestService.class, TestService.class.getSimpleName());
        testService = new TestServiceImpl();
        method = testService.getClass().getDeclaredMethod("greeting");
        feignDecorator = new TestFeignDecorator();

        methodHandler = mock(MethodHandler.class);
        when(methodHandler.invoke(any())).thenReturn(testService.greeting());

        dispatch = new HashMap<>();
        dispatch.put(method, methodHandler);
    }

    @Test
    public void testInvoke() throws Throwable {
        testSubject = new DecoratorInvocationHandler(target, dispatch, feignDecorator);
        final Object result = testSubject.invoke(testService, method, new Object[0]);

        verify(methodHandler, times(1)).invoke(any());

        assertThat(feignDecorator.isCalled())
                .describedAs("FeignDecorator is called")
                .isTrue();
        assertThat(result)
                .describedAs("Return of invocation")
                .isEqualTo(testService.greeting());
    }

    @Test
    public void testDecorator() throws Throwable {
        feignDecorator.setAlternativeFunction(fnArgs -> "AlternativeFunction");
        testSubject = new DecoratorInvocationHandler(target, dispatch, feignDecorator);
        final Object result = testSubject.invoke(testService, method, new Object[0]);

        verify(methodHandler, times(0)).invoke(any());
        assertThat(feignDecorator.isCalled())
                .describedAs("FeignDecorator is called")
                .isTrue();
        assertThat(result)
                .describedAs("Return of invocation")
                .isEqualTo("AlternativeFunction");
    }

}
