package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.ratelimiter.operator.RateLimiterOperator;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.ExecutableMethod;
import io.reactivex.Flowable;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RateLimiterSpecificationInterceptor implements MethodInterceptor<Object, Object> {

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimiterSpecificationInterceptor(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    private final Map<ExecutableMethod, TransactionInvocation> transactionInvocationMap = new ConcurrentHashMap<>(30);


    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String name = executableMethod.stringValue(RateLimiter.class).orElse("default");
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = this.rateLimiterRegistry.rateLimiter(name);

        ReturnType<Object> rt = context.getReturnType();
        Class<Object> returnType = rt.getType();
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            Object result = context.proceed();
            if (result == null) {
                return result;
            } else {
                // TODO: need to work out completition stage
                return io.github.resilience4j.ratelimiter.RateLimiter.decorateCompletionStage(rateLimiter, () -> ((CompletableFuture<?>) result));
            }
        } else if (Publishers.isConvertibleToPublisher(returnType)) {
            ConversionService<?> conversionService = ConversionService.SHARED;
            Object result = context.proceed();
            if (result == null) {
                return result;
            } else {
                Flowable<?> observable = conversionService
                    .convert(result, Flowable.class)
                    .orElseThrow(() -> new IllegalStateException("Unconvertible Reactive type: " + result));
                return observable.compose(RateLimiterOperator.of(rateLimiter));
            }
        }
        try {
            return io.github.resilience4j.ratelimiter.RateLimiter.decorateCheckedSupplier(rateLimiter, context::proceed).apply();
        } catch (Throwable throwable) {
            // TODO: need to call recovertable function
            return null;
        }
    }

    /**
     * Cached invocation associating a method with a definition a transaction manager.
     */
    private final class TransactionInvocation {

    }
}
