package io.github.resilience4j.fallback;

import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.spelresolver.SpelResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

public class FallbackExecutor {

    private static final Logger logger = LoggerFactory.getLogger(FallbackExecutor.class);

    private final SpelResolver spelResolver;
    private final FallbackDecorators fallbackDecorators;

    public FallbackExecutor(SpelResolver spelResolver, FallbackDecorators fallbackDecorators) {
        this.spelResolver = spelResolver;
        this.fallbackDecorators = fallbackDecorators;
    }

    public Object execute(ProceedingJoinPoint proceedingJoinPoint, Method method, String fallbackMethodValue, CheckedSupplier<Object> primaryFunction) throws Throwable {
        String fallbackMethodName = spelResolver.resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue);

        FallbackMethod fallbackMethod = null;
        if (StringUtils.hasLength(fallbackMethodName)) {
            try {
                fallbackMethod = FallbackMethod
                    .create(fallbackMethodName, method, proceedingJoinPoint.getArgs(), proceedingJoinPoint.getTarget().getClass(), proceedingJoinPoint.getThis());
            } catch (NoSuchMethodException ex) {
                logger.warn("No fallback method match found", ex);
            }
        }
        if (fallbackMethod == null) {
            return primaryFunction.get();
        } else {
            return fallbackDecorators.decorate(fallbackMethod, primaryFunction).get();
        }
    }
}
