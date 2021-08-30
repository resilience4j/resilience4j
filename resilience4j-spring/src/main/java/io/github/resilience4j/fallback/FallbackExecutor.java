package io.github.resilience4j.fallback;

import io.github.resilience4j.spelresolver.SpelResolver;
import io.vavr.CheckedFunction0;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

public class FallbackExecutor {
    private final SpelResolver spelResolver;
    private final FallbackDecorators fallbackDecorators;

    public FallbackExecutor(SpelResolver spelResolver, FallbackDecorators fallbackDecorators) {
        this.spelResolver = spelResolver;
        this.fallbackDecorators = fallbackDecorators;
    }

    public Object execute(ProceedingJoinPoint proceedingJoinPoint, Method method, String fallbackMethodValue, CheckedFunction0<Object> primaryFunction) throws Throwable {
        String fallbackMethodName = spelResolver.resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue);
        if (StringUtils.isEmpty(fallbackMethodName)) {
            return primaryFunction.apply();
        }
        FallbackMethod fallbackMethod = FallbackMethod
            .create(fallbackMethodName, method, proceedingJoinPoint.getArgs(), proceedingJoinPoint.getTarget());
        return fallbackDecorators.decorate(fallbackMethod, primaryFunction).apply();
    }
}
