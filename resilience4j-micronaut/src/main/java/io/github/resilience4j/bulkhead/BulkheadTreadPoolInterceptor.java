package io.github.resilience4j.bulkhead;

import io.github.resilience4j.BaseInterceptor;
import io.github.resilience4j.fallback.UnhandledFallbackException;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;
import io.micronaut.retry.intercept.RecoveryInterceptor;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link MethodInterceptor} that intercepts all method calls which are annotated with a {@link io.github.resilience4j.annotation.Bulkhead}
 * annotation.
 **/
@Singleton
@Requires(beans = ThreadPoolBulkheadRegistry.class)
public class BulkheadTreadPoolInterceptor  extends BaseInterceptor implements MethodInterceptor<Object,Object> {

    /**
     * Positioned before the {@link io.github.resilience4j.annotation.Bulkhead} interceptor after {@link io.micronaut.retry.annotation.Fallback}.
     */
    public static final int POSITION = RecoveryInterceptor.POSITION + 20;

    private final ThreadPoolBulkheadRegistry bulkheadRegistry;
    private final BeanContext beanContext;

    /**
     *
     * @param beanContext The bean context to allow for DI of class annotated with {@link javax.inject.Inject}.
     * @param bulkheadRegistry bulkhead registry used to retrieve {@link Bulkhead} by name
     */
    public BulkheadTreadPoolInterceptor(BeanContext beanContext,
                                        ThreadPoolBulkheadRegistry bulkheadRegistry) {
        this.bulkheadRegistry = bulkheadRegistry;
        this.beanContext = beanContext;
    }

    @Override
    public int getOrder() {
        return POSITION;
    }

    /**
     * Finds a fallback method for the given context.
     *
     * @param context The context
     * @return The fallback method if it is present
     */
    @Override
    public Optional<? extends MethodExecutionHandle<?, Object>> findFallbackMethod(MethodInvocationContext<Object, Object> context) {
        ExecutableMethod executableMethod = context.getExecutableMethod();
        final String fallbackMethod = executableMethod.stringValue(io.github.resilience4j.annotation.Bulkhead.class, "fallbackMethod").orElse("");
        Class<?> declaringType = context.getDeclaringType();
        return beanContext.findExecutionHandle(declaringType, fallbackMethod, context.getArgumentTypes());
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {

        Optional<AnnotationValue<io.github.resilience4j.annotation.Bulkhead>> opt = context.findAnnotation(io.github.resilience4j.annotation.Bulkhead.class);
        if (!opt.isPresent()) {
            return context.proceed();
        }
        final io.github.resilience4j.annotation.Bulkhead.Type type = opt.get().enumValue("type", io.github.resilience4j.annotation.Bulkhead.Type.class).orElse(io.github.resilience4j.annotation.Bulkhead.Type.SEMAPHORE);
        if(type != io.github.resilience4j.annotation.Bulkhead.Type.THREADPOOL) {
            return context.proceed();
        }
        final String name = opt.get().stringValue().orElse("default");
        ThreadPoolBulkhead bulkhead = this.bulkheadRegistry.bulkhead(name);
        ReturnType<Object> rt = context.getReturnType();
        Class<Object> returnType = rt.getType();
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            Object result = context.proceed();
            if (result == null) {
                return result;
            }
            return this.fallbackCompletable(bulkhead.executeSupplier(() -> ((CompletableFuture<?>) result)),context);
        } else if (Publishers.isConvertibleToPublisher(returnType)) {

            throw new IllegalStateException(
                "ThreadPool bulkhead is only applicable for completable futures ");

//            Object result = context.proceed();
//            if (result == null) {
//                return result;
//            }
//            Flowable<Object> flowable = ConversionService.SHARED
//                .convert(result, Flowable.class)
//                .orElseThrow(() -> new UnhandledFallbackException("Unsupported Reactive type: " + result));
//
//
//            flowable = this.fallbackFlowable(flowable.compose(BulkheadOperator.of(bulkhead)),context);
//            return ConversionService.SHARED
//                .convert(flowable, context.getReturnType().asArgument())
//                .orElseThrow(() -> new UnhandledFallbackException("Unsupported Reactive type: " + result));
        }
        try {
            return bulkhead.executeSupplier(context::proceed);
        } catch (RuntimeException exception) {
            return this.fallback(context, exception);
        } catch (Throwable throwable) {
            throw new UnhandledFallbackException("Error invoking fallback for type [" + context.getTarget().getClass().getName() + "]: " + throwable.getMessage(), throwable);
        }
    }
}
