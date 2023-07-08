/*
 * Copyright 2019 authors
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
package io.github.resilience4j.fallback;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * https://github.com/resilience4j/resilience4j/issues/653
 */
@SuppressWarnings("unused")
public class FallbackMethodParentTest {

    public String testMethod(String parameter) {
        return "test";
    }

    public String fallbackParent(String parameter, IllegalStateException exception) {
        return "recovered";
    }

    public String ambiguousFallback(String parameter, IOException exception) {
        return "not-only-exception";
    }

    @Test
    public void fallbackIgnoringParentMethod() throws Throwable {
        Proxy target = new Proxy();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        FallbackMethod fallbackMethod = FallbackMethod
            .create("fallbackParent", testMethod, new Object[]{"test"}, target.getClass(), target);

        assertThat(fallbackMethod.fallback(new IllegalStateException("err")))
            .isEqualTo("proxy-recovered");
    }

    @Test
    public void fallbackIgnoringInterfaceMethod() throws Throwable {
        Proxy target = new Proxy();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        FallbackMethod fallbackMethod = FallbackMethod
            .create("fallbackInterface", testMethod, new Object[]{"test"}, target.getClass(), target);

        assertThat(fallbackMethod.fallback(new IllegalArgumentException("err")))
            .isEqualTo("proxy-recovered");
    }

    @Test
    public void fallbackNotIgnoringInterfaceMethod() throws Throwable {
        Proxy target = new Proxy();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        FallbackMethod fallbackMethod = FallbackMethod
            .create("fallbackOnlyInterface", testMethod, new Object[]{"test"}, target.getClass(), target);

        assertThat(fallbackMethod.fallback(new IllegalArgumentException("err")))
            .isEqualTo("only-interface-recovered");
    }

    @Test
    public void dontFallbackAmbiguousMethod() throws Throwable {
        Proxy target = new Proxy();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        assertThatThrownBy(() -> FallbackMethod
            .create("ambiguousFallback", testMethod, new Object[]{"test"}, target.getClass(), target))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("You have more that one fallback method that cover the same exception type "
                + IOException.class.getName());
    }

    interface ProxyInterface {

        default String fallbackInterface(String parameter, IllegalArgumentException exception) {
            return "interface-recovered";
        }

        default String fallbackOnlyInterface(String parameter, IllegalArgumentException exception) {
            return "only-interface-recovered";
        }

    }

    class Proxy extends FallbackMethodParentTest implements ProxyInterface {

        @Override
        public String fallbackParent(String parameter, IllegalStateException exception) {
            return "proxy-recovered";
        }

        @Override
        public String fallbackInterface(String parameter, IllegalArgumentException exception) {
            return "proxy-recovered";
        }

        public String ambiguousFallback(IOException exception) {
            return "only-exception";
        }
    }
}