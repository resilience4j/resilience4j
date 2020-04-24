package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.operator.RateLimiterOperator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@Internal
@Requires(classes = RateLimiterRegistry.class)
public class RateLimiterSpecificationInterceptor implements MethodInterceptor<Object, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterSpecificationInterceptor.class);

    private final RateLimiterRegistry rateLimiterRegistry;
    private final BeanContext beanContext;

    public RateLimiterSpecificationInterceptor(BeanContext beanContext, RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.beanContext = beanContext;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String name = executableMethod.stringValue(RateLimiter.class).orElse("default");
        //TODO: need to implement fallback method
        final String fallbackMethod = executableMethod.stringValue(RateLimiter.class, "fallbackMethod").orElse("");
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = this.rateLimiterRegistry.rateLimiter(name);

//        TODO: need to work out execution handler
//        this.beanContext.findExecutionHandle()

        ReturnType<Object> rt = context.getReturnType();
        Class<Object> returnType = rt.getType();
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return invokeForCompletion(context, rateLimiter);
        } else if (Publishers.isConvertibleToPublisher(returnType)) {
            return invokeForPublisher(context, rateLimiter);
        }
        try {
            return io.github.resilience4j.ratelimiter.RateLimiter.decorateCheckedSupplier(rateLimiter, context::proceed).apply();
        } catch (Throwable throwable) {
            throw new RuntimeException("//TODO: need to implement");
        }

    }

    public Object invokeForCompletion(MethodInvocationContext<Object, Object> context, io.github.resilience4j.ratelimiter.RateLimiter rateLimiter) {
        Object result = context.proceed();
        if (result == null) {
            return result;
        }
        return rateLimiter.executeCompletionStage(() -> ((CompletableFuture<?>) result));
    }

    public Object invokeForPublisher(MethodInvocationContext<Object, Object> context, io.github.resilience4j.ratelimiter.RateLimiter rateLimiter) {
        ConversionService<?> conversionService = ConversionService.SHARED;
        Object result = context.proceed();
        if (result == null) {
            return result;
        }
        Flowable<?> observable = conversionService
            .convert(result, Flowable.class)
            .orElseThrow(() -> new IllegalStateException("Unconvertible Reactive type: " + result));
        return observable.compose(RateLimiterOperator.of(rateLimiter));
    }
}
