package io.github.resilience4j.fallback;

import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.spelresolver.SpelResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

public class FallbackExecutor implements BeanFactoryAware {

    private static final String BEAN_METHOD_SEPARATOR = "::";
    private static final Logger logger = LoggerFactory.getLogger(FallbackExecutor.class);

    private final SpelResolver spelResolver;
    private final FallbackDecorators fallbackDecorators;
    private BeanFactory beanFactory;

    public FallbackExecutor(SpelResolver spelResolver, FallbackDecorators fallbackDecorators) {
        this.spelResolver = spelResolver;
        this.fallbackDecorators = fallbackDecorators;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public Object execute(ProceedingJoinPoint proceedingJoinPoint, Method method, String fallbackMethodValue, CheckedSupplier<Object> primaryFunction) throws Throwable {
        String fallbackMethodName = spelResolver.resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue);

        FallbackMethod fallbackMethod = null;
        if (StringUtils.hasLength(fallbackMethodName)) {
            try {
                Object original;
                Object proxy;
                int separatorIdx = fallbackMethodName.indexOf(BEAN_METHOD_SEPARATOR);
                if (separatorIdx > 0 && beanFactory != null) {
                    String beanName = fallbackMethodName.substring(0, separatorIdx);
                    fallbackMethodName = fallbackMethodName.substring(separatorIdx + BEAN_METHOD_SEPARATOR.length());
                    if (!StringUtils.hasLength(fallbackMethodName)) {
                        throw new NoSuchMethodException(
                            "Invalid fallbackMethod format: expected 'beanName::methodName' but got '" + fallbackMethodValue + "'");
                    }
                    Object fallbackBean = beanFactory.getBean(beanName);
                    proxy = fallbackBean;
                    Object singletonTarget = AopProxyUtils.getSingletonTarget(fallbackBean);
                    original = (singletonTarget != null) ? singletonTarget : fallbackBean;
                } else {
                    original = proceedingJoinPoint.getTarget();
                    proxy = proceedingJoinPoint.getThis();
                }
                fallbackMethod = FallbackMethod
                    .create(fallbackMethodName, method, proceedingJoinPoint.getArgs(), original, proxy);
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
