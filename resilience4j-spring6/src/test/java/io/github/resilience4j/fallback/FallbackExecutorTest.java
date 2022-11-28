package io.github.resilience4j.fallback;

import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.spelresolver.SpelResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class FallbackExecutorTest {
    private final SpelResolver spelResolver = mock(SpelResolver.class);
    private final FallbackDecorators fallbackDecorators = mock(FallbackDecorators.class);
    private final ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);

    private final FallbackExecutor fallbackExecutor = new FallbackExecutor(spelResolver, fallbackDecorators);


    @Test
    public void testPrimaryMethodExecutionWithFallback() throws Throwable {
        Method method = this.getClass().getMethod("getName", String.class);
        final CheckedSupplier<Object> primaryFunction = () -> getName("Name");
        final String fallbackMethodValue = "getNameValidFallback";

        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
        when(proceedingJoinPoint.getTarget()).thenReturn(this);
        when(spelResolver.resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue)).thenReturn(fallbackMethodValue);
        when(fallbackDecorators.decorate(any(),eq(primaryFunction))).thenReturn(primaryFunction);

        final Object result = fallbackExecutor.execute(proceedingJoinPoint, method, fallbackMethodValue, primaryFunction);

        assertThat(result).isEqualTo("Name");

        verify(spelResolver, times(1)).resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue);
        verify(fallbackDecorators, times(1)).decorate(any(),eq(primaryFunction));
    }

    @Test
    public void testPrimaryMethodExecutionWithoutFallback() throws Throwable {
        Method method = this.getClass().getMethod("getName", String.class);
        final CheckedSupplier<Object> primaryFunction = () -> getName("Name");
        final String fallbackMethodValue = "";

        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
        when(proceedingJoinPoint.getTarget()).thenReturn(this);
        when(spelResolver.resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue)).thenReturn(fallbackMethodValue);
        when(fallbackDecorators.decorate(any(),eq(primaryFunction))).thenReturn(primaryFunction);

        final Object result = fallbackExecutor.execute(proceedingJoinPoint, method, fallbackMethodValue, primaryFunction);
        assertThat(result).isEqualTo("Name");

        verify(spelResolver, times(1)).resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue);
        verify(fallbackDecorators, never()).decorate(any(),any());
    }

    @Test
    public void testPrimaryMethodExecutionWithFallbackNotFound() throws Throwable {
        Method method = this.getClass().getMethod("getName", String.class);
        final CheckedSupplier<Object> primaryFunction = () -> getName("Name");
        final String fallbackMethodValue = "incorrectFallbackMethodName";

        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
        when(proceedingJoinPoint.getTarget()).thenReturn(this);
        when(spelResolver.resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue)).thenReturn(fallbackMethodValue);
        when(fallbackDecorators.decorate(any(),eq(primaryFunction))).thenReturn(primaryFunction);

        final Object result = fallbackExecutor.execute(proceedingJoinPoint, method, fallbackMethodValue, primaryFunction);
        assertThat(result).isEqualTo("Name");

        verify(spelResolver, times(1)).resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue);
        verify(fallbackDecorators, never()).decorate(any(),any());
    }

    @Test
    public void testPrimaryMethodExecutionWithFallbackWithIncorrectSignature() throws Throwable {
        Method method = this.getClass().getMethod("getName", String.class);
        final CheckedSupplier<Object> primaryFunction = () -> getName("Name");
        final String fallbackMethodValue = "getNameInvalidFallback";

        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
        when(proceedingJoinPoint.getTarget()).thenReturn(this);
        when(spelResolver.resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue)).thenReturn(fallbackMethodValue);
        when(fallbackDecorators.decorate(any(),eq(primaryFunction))).thenReturn(primaryFunction);

        final Object result = fallbackExecutor.execute(proceedingJoinPoint, method, fallbackMethodValue, primaryFunction);
        assertThat(result).isEqualTo("Name");

        verify(spelResolver, times(1)).resolve(method, proceedingJoinPoint.getArgs(), fallbackMethodValue);
        verify(fallbackDecorators, never()).decorate(any(),any());
    }

    public String getName(String name) {
        return name;
    }

    public String getNameInvalidFallback() {
        return "recovered-from-invalid-fallback";
    }

    public String getNameValidFallback(String parameter, Throwable throwable) {
        return "recovered-from-valid-fallback";
    }
}