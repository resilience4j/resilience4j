package io.github.resilience4j.spring6.fallback;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FallbackMethodProxyTest {

    @Test
    void shouldInvokeFinalFallbackOnTarget() throws Throwable {
        TestService target = new TestService(new Dependency());
        Service proxy = createProxy(target, true);
        FallbackMethod fallback = createFallback("fallback", target, proxy);
        assertThat(AopUtils.isCglibProxy(proxy)).isTrue();
        assertThat(proxy.operation("input")).isEqualTo("dependency:input");

        Object result = fallback.fallback(new IllegalArgumentException("error"));

        assertThat(result).isEqualTo("dependency:input:error");
    }

    @Test
    void shouldKeepAdviceOnNonFinalFallback() throws Throwable {
        TestService target = new TestService(new Dependency());
        Service proxy = createProxy(target, true);
        FallbackMethod fallback = createFallback("fallback", target, proxy);

        Object result = fallback.fallback(new RuntimeException("error"));

        assertThat(result).isEqualTo("advised:dependency:input:error");
    }

    @Test
    void shouldInvokeFinalGlobalFallbackOnTarget() throws Throwable {
        TestService target = new TestService(new Dependency());
        Service proxy = createProxy(target, true);
        FallbackMethod fallback = createFallback("globalFallback", target, proxy);

        Object result = fallback.fallback(new RuntimeException("error"));

        assertThat(result).isEqualTo("dependency:error");
    }

    @Test
    void shouldInvokeFinalFallbackWithNoOriginalArgumentsOnTarget() throws Throwable {
        TestService target = new TestService(new Dependency());
        Service proxy = createProxy(target, true);
        Method method = TestService.class.getMethod("operation");
        FallbackMethod fallback = FallbackMethod.create("globalFallback", method,
            new Object[0], target, proxy);

        Object result = fallback.fallback(new RuntimeException("error"));

        assertThat(result).isEqualTo("dependency:error");
    }

    @Test
    void shouldInvokePrivateFallbackOnTarget() throws Throwable {
        TestService target = new TestService(new Dependency());
        Service proxy = createProxy(target, true);
        FallbackMethod fallback = createFallback("privateFallback", target, proxy);

        Object result = fallback.fallback(new RuntimeException("error"));

        assertThat(result).isEqualTo("dependency:input:error");
    }

    @Test
    void shouldInvokeFallbackMissingFromJdkProxyInterfaceOnTarget() throws Throwable {
        TestService target = new TestService(new Dependency());
        Service proxy = createProxy(target, false);
        FallbackMethod fallback = createFallback("fallback", target, proxy);
        assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();

        Object result = fallback.fallback(new RuntimeException("error"));

        assertThat(result).isEqualTo("dependency:input:error");
    }

    @Test
    void shouldPropagateFinalFallbackExceptionWithoutWrapping() throws Throwable {
        TestService target = new TestService(new Dependency());
        Service proxy = createProxy(target, true);
        FallbackMethod fallback = createFallback("rethrowingFallback", target, proxy);
        RuntimeException exception = new RuntimeException("error");

        assertThatThrownBy(() -> fallback.fallback(exception)).isSameAs(exception);
    }

    private FallbackMethod createFallback(String name, TestService target, Service proxy)
        throws NoSuchMethodException {
        Method method = TestService.class.getMethod("operation", String.class);
        return FallbackMethod.create(name, method, new Object[]{"input"}, target, proxy);
    }

    private Service createProxy(TestService target, boolean proxyTargetClass) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(proxyTargetClass);
        factory.addAdvice((MethodInterceptor) invocation -> {
            Object result = invocation.proceed();
            return invocation.getMethod().getName().equals("fallback")
                ? "advised:" + result : result;
        });
        return (Service) factory.getProxy();
    }

    interface Service {
        String operation(String input);
    }

    static class TestService implements Service {
        private final Dependency dependency;

        TestService(Dependency dependency) {
            this.dependency = dependency;
        }

        @Override
        public String operation(String input) {
            return dependency.value() + ":" + input;
        }

        public String operation() {
            return dependency.value();
        }

        public final String fallback(String input, IllegalArgumentException exception) {
            return dependency.value() + ":" + input + ":" + exception.getMessage();
        }

        public String fallback(String input, RuntimeException exception) {
            return dependency.value() + ":" + input + ":" + exception.getMessage();
        }

        public final String globalFallback(RuntimeException exception) {
            return dependency.value() + ":" + exception.getMessage();
        }

        private String privateFallback(String input, RuntimeException exception) {
            return dependency.value() + ":" + input + ":" + exception.getMessage();
        }

        public final String rethrowingFallback(String input, RuntimeException exception) {
            dependency.value();
            throw exception;
        }
    }

    static class Dependency {
        String value() {
            return "dependency";
        }
    }
}
