/*
 * Copyright 2019 Olov Andersson
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
package io.github.resilience4j.circuitbreaker.configure;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties.InstanceProperties;


public class CircuitBreakerAnnotationConfigScannerTest {

	@Test
	public void testMergeConfigurationPropertiesAnnotationConfigUsed() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClassName(MethodAnnotatedBean.class.getName());
		beanFactory.registerBeanDefinition("foo", beanDefinition);
		CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner();
		scanner.postProcessBeanFactory(beanFactory);

		CircuitBreakerConfigurationProperties circuitBreakerProperties = new CircuitBreakerConfigurationProperties();
		scanner.mergeConfigurationProperties(circuitBreakerProperties);
		InstanceProperties instanceProperties = circuitBreakerProperties.getInstances().get("methodbreaker");
		assertNotNull(instanceProperties);
		assertArrayEquals(new Class[] { IllegalArgumentException.class }, instanceProperties.getIgnoreExceptions());
	}
	
	@Test
	public void testMergeConfigurationPropertiesPropertiesTakePrecedence() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClassName(MethodAnnotatedBean.class.getName());
		beanFactory.registerBeanDefinition("foo", beanDefinition);
		CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner();
		scanner.postProcessBeanFactory(beanFactory);

		CircuitBreakerConfigurationProperties circuitBreakerProperties = new CircuitBreakerConfigurationProperties();
		InstanceProperties existingInstanceProperties = new InstanceProperties();
		existingInstanceProperties.setIgnoreExceptions(new Class[] {NullPointerException.class});
		circuitBreakerProperties.getInstances().put("methodbreaker", existingInstanceProperties);
		scanner.mergeConfigurationProperties(circuitBreakerProperties);
		InstanceProperties instanceProperties = circuitBreakerProperties.getInstances().get("methodbreaker");
		assertNotNull(instanceProperties);
		assertArrayEquals(new Class[] { NullPointerException.class }, instanceProperties.getIgnoreExceptions());
	}

	@Test
	public void testClassAndMethodAnnotatedConfigUsed() {
	    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(ClassAndMethodAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("foo", beanDefinition);
        CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner();
        scanner.postProcessBeanFactory(beanFactory);

        CircuitBreakerConfigurationProperties circuitBreakerProperties = new CircuitBreakerConfigurationProperties();
        scanner.mergeConfigurationProperties(circuitBreakerProperties);
        
        InstanceProperties instanceProperties = circuitBreakerProperties.getInstances().get("classbreaker");
        assertNotNull(instanceProperties);
        assertArrayEquals(new Class[] { UnsupportedOperationException.class }, instanceProperties.getRecordExceptions());
        instanceProperties = circuitBreakerProperties.getInstances().get("methodbreaker");
        assertNotNull(instanceProperties);
        assertArrayEquals(new Class[] { IllegalArgumentException.class }, instanceProperties.getIgnoreExceptions());
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
        CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner();
        scanner.postProcessBeanFactory(beanFactory);
	}
	
	@Test
    public void testInterfaceMethodAnnotationUsed() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(InterfaceMethodAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("foo", beanDefinition);
        CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner();
        scanner.postProcessBeanFactory(beanFactory);

        CircuitBreakerConfigurationProperties circuitBreakerProperties = new CircuitBreakerConfigurationProperties();
        scanner.mergeConfigurationProperties(circuitBreakerProperties);
        InstanceProperties instanceProperties = circuitBreakerProperties.getInstances().get("interfacemethodbreaker");
        assertNotNull(instanceProperties);
        assertArrayEquals(new Class[] { NullPointerException.class }, instanceProperties.getRecordExceptions());
    }
	
   @Test
    public void testInterfaceTypeAnnotationUsed() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(InterfaceTypeAnnotatedBean.class.getName());
        beanFactory.registerBeanDefinition("foo", beanDefinition);
        CircuitBreakerAnnotationConfigScanner scanner = new CircuitBreakerAnnotationConfigScanner();
        scanner.postProcessBeanFactory(beanFactory);

        CircuitBreakerConfigurationProperties circuitBreakerProperties = new CircuitBreakerConfigurationProperties();
        scanner.mergeConfigurationProperties(circuitBreakerProperties);
        InstanceProperties instanceProperties = circuitBreakerProperties.getInstances().get("interfacetypebreaker");
        assertNotNull(instanceProperties);
        assertArrayEquals(new Class[] { IllegalStateException.class }, instanceProperties.getIgnoreExceptions());
    }
	
    public static class MethodAnnotatedBean {

        @CircuitBreaker(name = "methodbreaker", ignoreExceptions = {IllegalArgumentException.class})
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
        @CircuitBreaker( name = "interfacemethodbreaker", recordExceptions = {NullPointerException.class})
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
