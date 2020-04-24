package io.github.resilience4j.retry;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.transformer.RetryTransformer;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
@Internal
@Requires(classes = RetryRegistry.class)
public class RetrySpecificationInterceptor implements MethodInterceptor {
    private final RetryRegistry retryRegistry;
    private final BeanContext beanContext;
    private static final ScheduledExecutorService retryExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public RetrySpecificationInterceptor(BeanContext beanContext, RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
        this.beanContext = beanContext;
    }

    @Override
    public Object intercept(MethodInvocationContext context) {
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String name = executableMethod.stringValue(TimeLimiter.class).orElse("default");
        final String fallbackMethod = executableMethod.stringValue(RateLimiter.class, "fallbackMethod").orElse("");
        Retry retry = retryRegistry.retry(name);

        ReturnType<Object> rt = context.getReturnType();
        Class<Object> returnType = rt.getType();
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            Object result = context.proceed();
            if(result == null){
                return result;
            }
            return retry.executeCompletionStage(retryExecutorService, () -> ((CompletableFuture<?>) result));
        } else if (Publishers.isConvertibleToPublisher(returnType)) {
            ConversionService<?> conversionService = ConversionService.SHARED;
            Object result = context.proceed();
            if (result == null) {
                return result;
            }
            Flowable<?> observable = conversionService
                .convert(result, Flowable.class)
                .orElseThrow(() -> new IllegalStateException("Unconvertible Reactive type: " + result));
            return observable.compose(RetryTransformer.of(retry));
        }
        return null;
    }
}
