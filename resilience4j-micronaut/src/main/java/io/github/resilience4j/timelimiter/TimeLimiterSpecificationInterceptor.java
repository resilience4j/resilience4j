package io.github.resilience4j.timelimiter;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.github.resilience4j.timelimiter.transformer.TimeLimiterTransformer;
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
@Requires(classes = TimeLimiterRegistry.class)
public class TimeLimiterSpecificationInterceptor implements MethodInterceptor<Object,Object> {
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final BeanContext beanContext;
    private static final ScheduledExecutorService timeLimiterExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public TimeLimiterSpecificationInterceptor(BeanContext beanContext, TimeLimiterRegistry timeLimiterRegistry) {
        this.beanContext = beanContext;
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String name = executableMethod.stringValue(TimeLimiter.class).orElse("default");
        final String fallbackMethod = executableMethod.stringValue(RateLimiter.class, "fallbackMethod").orElse("");

        io.github.resilience4j.timelimiter.TimeLimiter timeLimiter = this.timeLimiterRegistry.timeLimiter(name);

        ReturnType<Object> rt = context.getReturnType();
        Class<Object> returnType = rt.getType();
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            Object result = context.proceed();
            if(result == null){
                return result;
            }
            return timeLimiter.executeCompletionStage(timeLimiterExecutorService,() -> ((CompletableFuture<?>) result));

        } else if (Publishers.isConvertibleToPublisher(returnType)) {
            ConversionService<?> conversionService = ConversionService.SHARED;
            Object result = context.proceed();
            if (result == null) {
                return result;
            }
            Flowable<?> observable = conversionService
                .convert(result, Flowable.class)
                .orElseThrow(() -> new IllegalStateException("Unconvertible Reactive type: " + result));
            return observable.compose(TimeLimiterTransformer.of(timeLimiter));
        }
        return  null;
    }
}
