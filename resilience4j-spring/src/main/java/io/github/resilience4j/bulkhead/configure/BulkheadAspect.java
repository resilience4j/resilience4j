/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.utils.AnnotationExtractor;
import io.github.resilience4j.utils.ValueResolver;
import io.vavr.CheckedFunction0;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a {@link Bulkhead}
 * annotation. The aspect will handle methods that return a RxJava2 reactive type, Spring Reactor
 * reactive type, CompletionStage type, or value type.
 * <p>
 * The BulkheadRegistry is used to retrieve an instance of a Bulkhead for a specific name.
 * <p>
 * Given a method like this:
 * <pre><code>
 *     {@literal @}Bulkhead(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre>
 * each time the {@code #fancyName(String)} method is invoked, the method's execution will pass
 * through a a {@link io.github.resilience4j.bulkhead.Bulkhead} according to the given config.
 * <p>
 * The fallbackMethod parameter signature must match either:
 * <p>
 * 1) The method parameter signature on the annotated method or 2) The method parameter signature
 * with a matching exception type as the last parameter on the annotated method
 */
@Aspect
public class BulkheadAspect implements EmbeddedValueResolverAware, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(BulkheadAspect.class);

    private final BulkheadConfigurationProperties bulkheadConfigurationProperties;
    private final BulkheadRegistry bulkheadRegistry;
    private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
    private final @Nullable
    List<BulkheadAspectExt> bulkheadAspectExts;
    private final FallbackDecorators fallbackDecorators;
    private StringValueResolver embeddedValueResolver;

    public BulkheadAspect(BulkheadConfigurationProperties backendMonitorPropertiesRegistry,
        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry, BulkheadRegistry bulkheadRegistry,
        @Autowired(required = false) List<BulkheadAspectExt> bulkheadAspectExts,
        FallbackDecorators fallbackDecorators) {
        this.bulkheadConfigurationProperties = backendMonitorPropertiesRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.bulkheadAspectExts = bulkheadAspectExts;
        this.fallbackDecorators = fallbackDecorators;
        this.threadPoolBulkheadRegistry = threadPoolBulkheadRegistry;
    }

    @Pointcut(value = "@within(Bulkhead) || @annotation(Bulkhead)", argNames = "Bulkhead")
    public void matchAnnotatedClassOrMethod(Bulkhead Bulkhead) {
    }

    @Around(value = "matchAnnotatedClassOrMethod(bulkheadAnnotation)", argNames = "proceedingJoinPoint, bulkheadAnnotation")
    public Object bulkheadAroundAdvice(ProceedingJoinPoint proceedingJoinPoint,
        @Nullable Bulkhead bulkheadAnnotation) throws Throwable {
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
        if (bulkheadAnnotation == null) {
            bulkheadAnnotation = geBulkheadAnnotation(proceedingJoinPoint);
        }
        if (bulkheadAnnotation == null) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        Class<?> returnType = method.getReturnType();
        String backend = bulkheadAnnotation.name();
        String fallbackMethodValue = ValueResolver.resolve(this.embeddedValueResolver, bulkheadAnnotation.fallbackMethod());
        if (bulkheadAnnotation.type() == Bulkhead.Type.THREADPOOL) {
            if (StringUtils.isEmpty(fallbackMethodValue)) {
                return proceedInThreadPoolBulkhead(proceedingJoinPoint, methodName, returnType,
                    backend);
            }
            return executeFallBack(proceedingJoinPoint, fallbackMethodValue, method,
                () -> proceedInThreadPoolBulkhead(proceedingJoinPoint, methodName, returnType,
                    backend));
        } else {
            io.github.resilience4j.bulkhead.Bulkhead bulkhead = getOrCreateBulkhead(methodName,
                backend);
            if (StringUtils.isEmpty(fallbackMethodValue)) {
                return proceed(proceedingJoinPoint, methodName, bulkhead, returnType);
            }
            return executeFallBack(proceedingJoinPoint, fallbackMethodValue, method,
                () -> proceed(proceedingJoinPoint, methodName, bulkhead, returnType));
        }

    }

    private Object executeFallBack(ProceedingJoinPoint proceedingJoinPoint, String fallBackMethod,
        Method method, CheckedFunction0<Object> bulkhead) throws Throwable {
        FallbackMethod fallbackMethod = FallbackMethod
            .create(fallBackMethod, method, proceedingJoinPoint.getArgs(),
                proceedingJoinPoint.getTarget());
        return fallbackDecorators.decorate(fallbackMethod, bulkhead).apply();
    }

    /**
     * entry logic for semaphore bulkhead execution
     *
     * @param proceedingJoinPoint AOP proceedingJoinPoint
     * @param methodName          AOP method name
     * @param bulkhead            the configured bulkhead
     * @param returnType          the AOP method return type
     * @return the result Object of the method call
     * @throws Throwable
     */
    private Object proceed(ProceedingJoinPoint proceedingJoinPoint, String methodName,
        io.github.resilience4j.bulkhead.Bulkhead bulkhead, Class<?> returnType) throws Throwable {
        if (bulkheadAspectExts != null && !bulkheadAspectExts.isEmpty()) {
            for (BulkheadAspectExt bulkHeadAspectExt : bulkheadAspectExts) {
                if (bulkHeadAspectExt.canHandleReturnType(returnType)) {
                    return bulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, methodName);
                }
            }
        }
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return handleJoinPointCompletableFuture(proceedingJoinPoint, bulkhead);
        }
        return handleJoinPoint(proceedingJoinPoint, bulkhead);
    }

    private io.github.resilience4j.bulkhead.Bulkhead getOrCreateBulkhead(String methodName,
        String backend) {
        io.github.resilience4j.bulkhead.Bulkhead bulkhead = bulkheadRegistry.bulkhead(backend);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Created or retrieved bulkhead '{}' with max concurrent call '{}' and max wait time '{}ms' for method: '{}'",
                backend, bulkhead.getBulkheadConfig().getMaxConcurrentCalls(),
                bulkhead.getBulkheadConfig().getMaxWaitDuration().toMillis(), methodName);
        }

        return bulkhead;
    }

    /**
     * @param proceedingJoinPoint AOP proceedingJoinPoint
     * @return Bulkhead annotation
     */
    @Nullable
    private Bulkhead geBulkheadAnnotation(ProceedingJoinPoint proceedingJoinPoint) {
        if (logger.isDebugEnabled()) {
            logger.debug("bulkhead parameter is null");
        }
        if (proceedingJoinPoint.getTarget() instanceof Proxy) {
            logger
                .debug("The bulkhead annotation is kept on a interface which is acting as a proxy");
            return AnnotationExtractor
                .extractAnnotationFromProxy(proceedingJoinPoint.getTarget(), Bulkhead.class);
        } else {
            return AnnotationExtractor
                .extract(proceedingJoinPoint.getTarget().getClass(), Bulkhead.class);
        }
    }

    /**
     * Sync bulkhead execution
     *
     * @param proceedingJoinPoint AOP proceedingJoinPoint
     * @param bulkhead            the configured bulkhead for that backend call
     * @return the result object
     * @throws Throwable
     */
    private Object handleJoinPoint(ProceedingJoinPoint proceedingJoinPoint,
        io.github.resilience4j.bulkhead.Bulkhead bulkhead) throws Throwable {
        return bulkhead.executeCheckedSupplier(proceedingJoinPoint::proceed);
    }

    /**
     * handle the asynchronous completable future flow
     *
     * @param proceedingJoinPoint AOPJoinPoint
     * @param bulkhead            configured bulkhead
     * @return CompletionStage
     */
    private Object handleJoinPointCompletableFuture(ProceedingJoinPoint proceedingJoinPoint,
        io.github.resilience4j.bulkhead.Bulkhead bulkhead) {
        return bulkhead.executeCompletionStage(() -> {
            try {
                return (CompletionStage<?>) proceedingJoinPoint.proceed();
            } catch (Throwable throwable) {
                throw new CompletionException(throwable);
            }
        });
    }

    /**
     * execute the logic wrapped by ThreadPool bulkhead , please check {@link
     * io.github.resilience4j.bulkhead.ThreadPoolBulkhead} for more information
     *
     * @param proceedingJoinPoint AOP proceedingJoinPoint
     * @param methodName          AOP method name
     * @param returnType          AOP method return type
     * @param backend             backend name
     * @return result Object which will be CompletableFuture instance
     * @throws Throwable
     */
    private Object proceedInThreadPoolBulkhead(ProceedingJoinPoint proceedingJoinPoint,
        String methodName, Class<?> returnType, String backend) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("ThreadPool bulkhead invocation for method {} in backend {}", methodName,
                backend);
        }
        ThreadPoolBulkhead threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(backend);
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            return threadPoolBulkhead.executeSupplier(() -> {
                try {
                    return ((CompletionStage<?>) proceedingJoinPoint.proceed())
                        .toCompletableFuture().get();
                } catch (Throwable throwable) {
                    throw new CompletionException(throwable);
                }
            });
        } else {
            throw new IllegalStateException(
                "ThreadPool bulkhead is only applicable for completable futures ");
        }
    }


    @Override
    public int getOrder() {
        return bulkheadConfigurationProperties.getBulkheadAspectOrder();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }
}
