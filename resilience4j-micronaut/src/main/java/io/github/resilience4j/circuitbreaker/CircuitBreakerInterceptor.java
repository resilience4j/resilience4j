package io.github.resilience4j.circuitbreaker;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;

import javax.inject.Singleton;


@Singleton
@Internal
@Requires(classes = CircuitBreakerRegistry.class)
public class CircuitBreakerInterceptor implements MethodInterceptor<Object,Object> {
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BeanContext beanContext;

    public CircuitBreakerInterceptor(BeanContext beanContext, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.beanContext = beanContext;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        return null;
    }
}
