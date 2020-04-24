package io.github.resilience4j.bulkhead;

import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;

import javax.inject.Singleton;


@Singleton
@Internal
@Requires(classes = BulkheadRegistry.class)
public class BulkheadSpecificationInterceptor implements MethodInterceptor<Object,Object> {
    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {

        return null;
    }
}
