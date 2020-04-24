package io.github.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.bulkhead.operator.BulkheadOperator;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.ExecutableMethod;
import io.reactivex.Flowable;

import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


@Singleton
@Internal
@Requires(classes = BulkheadRegistry.class)
public class BulkheadSpecificationInterceptor implements MethodInterceptor<Object,Object> {

    private final BulkheadRegistry bulkheadRegistry;
    private final BeanContext beanContext;

    public BulkheadSpecificationInterceptor(BeanContext beanContext, BulkheadRegistry bulkheadRegistry) {
        this.bulkheadRegistry = bulkheadRegistry;
        this.beanContext = beanContext;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String name = executableMethod.stringValue(Bulkhead.class).orElse("default");
        final String fallbackMethod = executableMethod.stringValue(Bulkhead.class, "fallbackMethod").orElse("");

        io.github.resilience4j.bulkhead.Bulkhead bulkhead = this.bulkheadRegistry.bulkhead(name);
        ReturnType<Object> rt = context.getReturnType();
        Class<Object> returnType = rt.getType();
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            Object result = context.proceed();
            if(result == null) {
                return result;
            }
            return bulkhead.executeCompletionStage(() -> ((CompletableFuture<?>)result));
        } else if (Publishers.isConvertibleToPublisher(returnType)) {
            ConversionService<?> conversionService = ConversionService.SHARED;
            Object result = context.proceed();
            if (result == null) {
                return result;
            }
            Flowable<?> observable = conversionService
                .convert(result, Flowable.class)
                .orElseThrow(() -> new IllegalStateException("Unconvertible Reactive type: " + result));
            return observable.compose(BulkheadOperator.of(bulkhead));
        }
        return null;
    }
}
