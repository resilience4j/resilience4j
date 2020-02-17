package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.fallback.FallbackMethod;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.utils.ProceedingJoinPointWrapper;
import io.github.resilience4j.utils.ValueResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.util.List;

public class RateLimiterDecorator implements EmbeddedValueResolverAware {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterDecorator.class);
    private final RateLimiterRegistry rateLimiterRegistry;
    private final @Nullable
    List<RateLimiterDecoratorExt> rateLimiterAspectExtList;
    private final FallbackDecorators fallbackDecorators;
    private @Nullable StringValueResolver resolver;

    RateLimiterDecorator(
        RateLimiterRegistry rateLimiterRegistry,
        @Nullable List<RateLimiterDecoratorExt> rateLimiterAspectExtList,
        FallbackDecorators fallbackDecorators) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.rateLimiterAspectExtList = rateLimiterAspectExtList;
        this.fallbackDecorators = fallbackDecorators;
    }

    public ProceedingJoinPointWrapper decorate(
        ProceedingJoinPointWrapper joinPointHelper,
        RateLimiter rateLimiterAnnotation) throws Throwable {
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = getOrCreateRateLimiter(
            joinPointHelper.getDeclaringMethodName(), rateLimiterAnnotation.name());

        joinPointHelper = decorateWithoutFallback(joinPointHelper, rateLimiter);

        if (!StringUtils.isEmpty(rateLimiterAnnotation.fallbackMethod())) {
            FallbackMethod fallbackMethod = getFallbackMethod(joinPointHelper, rateLimiterAnnotation);
            joinPointHelper =  joinPointHelper.decorate(
                proceedCall -> fallbackDecorators.decorate(fallbackMethod, proceedCall));
        }

        return joinPointHelper;

    }

    private FallbackMethod getFallbackMethod(ProceedingJoinPointWrapper joinPointHelper,
                                             RateLimiter rateLimiterAnnotation) throws NoSuchMethodException {
        String fallbackMethodValue = ValueResolver
            .resolve(this.resolver, rateLimiterAnnotation.fallbackMethod());

        return FallbackMethod.create(
            fallbackMethodValue,
            joinPointHelper.getMethod(),
            joinPointHelper.getProceedingJoinPoint().getArgs(),
            joinPointHelper.getProceedingJoinPoint().getTarget());
    }

    private ProceedingJoinPointWrapper decorateWithoutFallback(
        ProceedingJoinPointWrapper joinPointHelper,
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter) {
        Class<?> returnType = joinPointHelper.getReturnType();

        if (rateLimiterAspectExtList != null && !rateLimiterAspectExtList.isEmpty()) {
            for (RateLimiterDecoratorExt rateLimiterAspectExt : rateLimiterAspectExtList) {
                if (rateLimiterAspectExt.canDecorateReturnType(returnType)) {
                    return joinPointHelper.decorate(function ->
                        rateLimiterAspectExt.decorate(rateLimiter, function));
                }
            }
        }

        return joinPointHelper.decorate(rateLimiter::decorateCheckedSupplier);
    }

    private io.github.resilience4j.ratelimiter.RateLimiter getOrCreateRateLimiter(
        String methodName, String name) {
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter
            = rateLimiterRegistry.rateLimiter(name);

        if (logger.isDebugEnabled()) {
            RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
            logger.debug(
                "Created or retrieved rate limiter '{}' with period: '{}'; "
                    + "limit for period: '{}'; timeout: '{}'; method: '{}'",
                name, rateLimiterConfig.getLimitRefreshPeriod(),
                rateLimiterConfig.getLimitForPeriod(),
                rateLimiterConfig.getTimeoutDuration(), methodName
            );
        }

        return rateLimiter;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }
}