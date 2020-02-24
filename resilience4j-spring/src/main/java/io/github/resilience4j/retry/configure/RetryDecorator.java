package io.github.resilience4j.retry.configure;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.utils.ProceedingJoinPointWrapper;
import io.github.resilience4j.utils.ValueResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.util.List;

public class RetryDecorator implements EmbeddedValueResolverAware {

    private static final Logger logger = LoggerFactory.getLogger(RetryDecorator.class);
    private final RetryRegistry retryRegistry;
    private final @Nullable
    List<RetryDecoratorExt> retryDecoratorExtList;
    private final FallbackDecorators fallbackDecorators;
    private @Nullable StringValueResolver resolver;

    RetryDecorator(
        RetryRegistry retryRegistry,
        @Nullable List<RetryDecoratorExt> retryDecoratorExtList,
        FallbackDecorators fallbackDecorators) {
        this.retryRegistry = retryRegistry;
        this.retryDecoratorExtList = retryDecoratorExtList;
        this.fallbackDecorators = fallbackDecorators;
    }

    public ProceedingJoinPointWrapper decorate(
        ProceedingJoinPointWrapper joinPointHelper,
        Retry retryAnnotation) throws NoSuchMethodException {
        io.github.resilience4j.retry.Retry retry = getOrCreateRetry(
            joinPointHelper.getDeclaringMethodName(), retryAnnotation.name());

        joinPointHelper = decorateWithoutFallback(joinPointHelper, retry);

        if (!StringUtils.isEmpty(retryAnnotation.fallbackMethod())) {
            FallbackMethod fallbackMethod = getFallbackMethod(joinPointHelper, retryAnnotation);
            joinPointHelper =  joinPointHelper.decorate(
                proceedCall -> fallbackDecorators.decorate(fallbackMethod, proceedCall));
        }

        return joinPointHelper;

    }

    private FallbackMethod getFallbackMethod(ProceedingJoinPointWrapper joinPointHelper,
                                             Retry annotation) throws NoSuchMethodException {
        String fallbackMethodValue = ValueResolver
            .resolve(this.resolver, annotation.fallbackMethod());

        return FallbackMethod.create(
            fallbackMethodValue,
            joinPointHelper.getMethod(),
            joinPointHelper.getProceedingJoinPoint().getArgs(),
            joinPointHelper.getProceedingJoinPoint().getTarget());
    }

    private ProceedingJoinPointWrapper decorateWithoutFallback(
        ProceedingJoinPointWrapper joinPointHelper,
        io.github.resilience4j.retry.Retry retry) {
        Class<?> returnType = joinPointHelper.getReturnType();

        if (retryDecoratorExtList != null && !retryDecoratorExtList.isEmpty()) {
            for (RetryDecoratorExt rateLimiterAspectExt : retryDecoratorExtList) {
                if (rateLimiterAspectExt.canDecorateReturnType(returnType)) {
                    return joinPointHelper.decorate(function ->
                        rateLimiterAspectExt.decorate(retry, function));
                }
            }
        }

        return joinPointHelper.decorate(retry::decorateCheckedSupplier);
    }

    private io.github.resilience4j.retry.Retry getOrCreateRetry(
        String methodName, String name) {
        io.github.resilience4j.retry.Retry retry
            = retryRegistry.retry(name);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Created or retrieved retry '{}' with max attempts rate '{}'  for method: '{}'",
                name, retry.getRetryConfig().getResultPredicate(), methodName);
        }
        return retry;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }
}