/*
 * Copyright 2019 Olov Andersson
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
 */
package io.github.resilience4j.circuitbreaker.configure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;


public class CircuitBreakerAnnotationConfigScannerTest {

    ConfigurableEnvironment env;
    MutablePropertySources propertySources = new MutablePropertySources();
    CircuitBreakerAnnotationConfigScanner scanner;

    @Before
    public void setUp() {
        env = mock(ConfigurableEnvironment.class);
        when(env.getPropertySources()).thenReturn(propertySources);
        scanner = new CircuitBreakerAnnotationConfigScanner(env);
    }

    @Test
    public void testTwoExceptionsOnMethodAnnotationConfigUsed() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(MethodAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("foo", beanDefinition);
        scanner.postProcessBeanFactory(beanFactory);

        PropertySource<?> propertySource = propertySources.get("circuitBreakerAnnotationConfig");
        assertNotNull(propertySource);
        assertEquals(IllegalArgumentException.class.getName(), propertySource.getProperty(getExceptionsPropertyName("methodbreaker", "ignoreExceptions", 0)));
        assertEquals(NullPointerException.class.getName(), propertySource.getProperty(getExceptionsPropertyName("methodbreaker", "ignoreExceptions", 1)));
    }

    @Test
    public void testClassAndMethodAnnotatedConfigUsed() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(ClassAndMethodAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("foo", beanDefinition);
        scanner.postProcessBeanFactory(beanFactory);

        PropertySource<?> propertySource = propertySources.get("circuitBreakerAnnotationConfig");
        assertNotNull(propertySource);
        assertEquals(UnsupportedOperationException.class.getName(), propertySource.getProperty(getRecordExceptionsPropertyName("classbreaker")));
        assertEquals(IllegalArgumentException.class.getName(), propertySource.getProperty(getIgnoreExceptionsPropertyName("methodbreaker")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConflictingAnnotationsThrowsIllegalArgumentException() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(ClassAndMethodAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("foo", beanDefinition);
        beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(MethodAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("bar", beanDefinition);
        scanner.postProcessBeanFactory(beanFactory);
    }

    @Test
    public void testNonConflictingAnnotationsDeclaredTwiceAreAllowed() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(MethodAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("foo", beanDefinition);
        beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(MethodAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("bar", beanDefinition);
        scanner.postProcessBeanFactory(beanFactory);
    }

    @Test
    public void testInterfaceMethodAnnotationUsed() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(InterfaceMethodAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("foo", beanDefinition);
        scanner.postProcessBeanFactory(beanFactory);

        PropertySource<?> propertySource = propertySources.get("circuitBreakerAnnotationConfig");
        assertNotNull(propertySource);
        assertEquals(NullPointerException.class.getName(), propertySource.getProperty(getRecordExceptionsPropertyName("interfacemethodbreaker")));
    }

    @Test
    public void testInterfaceTypeAnnotationUsed() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(InterfaceTypeAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("foo", beanDefinition);
        CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner(env);
        scanner.postProcessBeanFactory(beanFactory);

        PropertySource<?> propertySource = propertySources.get("circuitBreakerAnnotationConfig");
        assertNotNull(propertySource);
        assertEquals(IllegalStateException.class.getName(), propertySource.getProperty(getIgnoreExceptionsPropertyName("interfacetypebreaker")));
    }

    private String getIgnoreExceptionsPropertyName(String instanceName) {
        return getExceptionsPropertyName(instanceName, "ignoreExceptions", 0);
    }

    private String getRecordExceptionsPropertyName(String instanceName) {
        return getExceptionsPropertyName(instanceName, "recordExceptions", 0);
    }

    private String getExceptionsPropertyName(String instanceName, String exceptionsPropertyName, int index) {
        return String.format("resilience4j.circuitbreaker.instances.%s.%s[%d]", instanceName, exceptionsPropertyName, index);
    }

    public static class MethodAnnotatedBean {

        @CircuitBreaker(name = "methodbreaker", ignoreExceptions = {IllegalArgumentException.class, NullPointerException.class})
        public String foo() {
            return null;
        }
    }

    @CircuitBreaker(name = "classbreaker", recordExceptions = {UnsupportedOperationException.class})
    public static class ClassAndMethodAnnotatedBean {

        public String breakerMethod1() {
            return null;
        }

        @CircuitBreaker(name = "methodbreaker", ignoreExceptions = {IllegalArgumentException.class})
        public void breakerMethod2() {
        }
    }

    public static interface MethodAnnotatedInterface {
        @CircuitBreaker(name = "interfacemethodbreaker", recordExceptions = {NullPointerException.class})
        void breakerMethod();
    }

    @CircuitBreaker(name = "interfacetypebreaker", ignoreExceptions = {IllegalStateException.class})
    public static interface TypeAnnotatedInterface {
        void breakerMethod();
    }

    public static class InterfaceMethodAnnotatedBean implements MethodAnnotatedInterface {
        @Override
        public void breakerMethod() {
        }
    }

    public static abstract class AbstractInterfaceTypedAnnotatedBean implements TypeAnnotatedInterface {
    }

    public static class InterfaceTypeAnnotatedBean extends AbstractInterfaceTypedAnnotatedBean {
        @Override
        public void breakerMethod() {
        }
    }

}
