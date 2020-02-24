package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.utils.ProceedingJoinPointWrapper;
import io.github.resilience4j.utils.ValueResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.util.List;

public class CircuitBreakerDecorator implements EmbeddedValueResolverAware {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerDecorator.class);
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final @Nullable
    List<CircuitBreakerDecoratorExt> circuitBreakerDecoratorExtList;
    private final FallbackDecorators fallbackDecorators;
    private @Nullable StringValueResolver resolver;

    CircuitBreakerDecorator(
        CircuitBreakerRegistry circuitBreakerRegistry,
        @Nullable List<CircuitBreakerDecoratorExt> circuitBreakerDecoratorExtList,
        FallbackDecorators fallbackDecorators) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.circuitBreakerDecoratorExtList = circuitBreakerDecoratorExtList;
        this.fallbackDecorators = fallbackDecorators;
    }


    public ProceedingJoinPointWrapper decorate(
        ProceedingJoinPointWrapper joinPointHelper,
        CircuitBreaker circuitBreakerAnnotation) throws Throwable {
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(
            joinPointHelper.getDeclaringMethodName(), circuitBreakerAnnotation.name());

        joinPointHelper = decorateWithoutFallback(joinPointHelper, circuitBreaker);

        if (!StringUtils.isEmpty(circuitBreakerAnnotation.fallbackMethod())) {
            FallbackMethod fallbackMethod = getFallbackMethod(joinPointHelper, circuitBreakerAnnotation);
            joinPointHelper =  joinPointHelper.decorate(
                proceedCall -> fallbackDecorators.decorate(fallbackMethod, proceedCall));
        }

        return joinPointHelper;

    }

    private FallbackMethod getFallbackMethod(ProceedingJoinPointWrapper joinPointHelper,
                                             CircuitBreaker circuitBreakerAnnotation) throws NoSuchMethodException {
        String fallbackMethodValue = ValueResolver
            .resolve(this.resolver, circuitBreakerAnnotation.fallbackMethod());

        return FallbackMethod.create(
            fallbackMethodValue,
            joinPointHelper.getMethod(),
            joinPointHelper.getProceedingJoinPoint().getArgs(),
            joinPointHelper.getProceedingJoinPoint().getTarget());
    }

    private ProceedingJoinPointWrapper decorateWithoutFallback(
        ProceedingJoinPointWrapper joinPointHelper,
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) {
        Class<?> returnType = joinPointHelper.getReturnType();

        if (circuitBreakerDecoratorExtList != null && !circuitBreakerDecoratorExtList.isEmpty()) {
            for (CircuitBreakerDecoratorExt circuitBreakerDecoratorExt : circuitBreakerDecoratorExtList) {
                if (circuitBreakerDecoratorExt.canDecorateReturnType(returnType)) {
                    return joinPointHelper.decorate(function ->
                        circuitBreakerDecoratorExt.decorate(circuitBreaker, function));
                }
            }
        }

        return joinPointHelper.decorate(circuitBreaker::decorateCheckedSupplier);
    }

    private io.github.resilience4j.circuitbreaker.CircuitBreaker getOrCreateCircuitBreaker(
        String methodName, String name) {
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker(name);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Created or retrieved circuit breaker '{}' with failure rate '{}' for method: '{}'",
                name, circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(),
                methodName);
        }

        return circuitBreaker;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }
}
