package io.github.resilience4j.ratpack.internal;

import com.google.common.base.Strings;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratpack.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.ratpack.recovery.RecoveryFunction;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractMethodInterceptor implements MethodInterceptor {

    @Nullable
    protected Object proceed(MethodInvocation invocation) throws Throwable {
        Class<?> returnType = invocation.getMethod().getReturnType();
        Object result;
        try {
            result = invocation.proceed();
        } catch (Exception e) {
            if (Promise.class.isAssignableFrom(returnType)) {
                return Promise.error(e);
            } else if (Flux.class.isAssignableFrom(returnType)) {
                return Flux.error(e);
            } else if (Mono.class.isAssignableFrom(returnType)) {
                return Mono.error(e);
            } else if (CompletionStage.class.isAssignableFrom(returnType)) {
                CompletableFuture<?> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            } else {
                throw e;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected void completeFailedFuture(Throwable throwable, RecoveryFunction<?> fallbackMethod,
        CompletableFuture promise) {
        try {
            Object maybeFuture = fallbackMethod.apply(throwable);
            if (maybeFuture instanceof CompletionStage) {
                ((CompletionStage) maybeFuture).whenComplete((v1, t1) -> promise.complete(v1));
            } else {
                promise.complete(maybeFuture);
            }
        } catch (Exception e) {
            promise.completeExceptionally(e);
        }
    }

    @Nullable
    protected RecoveryFunction<?> createRecoveryFunction(MethodInvocation invocation,
        String fallback) {
        if (!Strings.isNullOrEmpty(fallback)) {
            Class<?>[] currentParamTypes = invocation.getMethod().getParameterTypes();
            return (throwable) -> {
                Class<?>[] currentParamTypesWithException = Arrays
                    .copyOf(currentParamTypes, currentParamTypes.length + 1);
                currentParamTypesWithException[currentParamTypesWithException.length
                    - 1] = throwable.getClass();
                try {
                    Method fallbackMethodWithException = findMethod(invocation, fallback,
                        currentParamTypesWithException);
                    Object[] args = Arrays
                        .copyOf(invocation.getArguments(), invocation.getArguments().length + 1);
                    args[args.length - 1] = throwable;
                    return fallbackMethodWithException.invoke(invocation.getThis(), args);
                } catch (NoSuchMethodException e1) {
                    try {
                        Method fallbackMethodWithoutException = findMethod(invocation, fallback,
                            currentParamTypes);
                        return fallbackMethodWithoutException
                            .invoke(invocation.getThis(), invocation.getArguments());
                    } catch (NoSuchMethodException e2) {
                        return new DefaultRecoveryFunction<>().apply(throwable);
                    }
                }
            };
        } else {
            return null;
        }
    }

    /**
     * Helper method to find a method where parameters are allowed to be instances of a superclass.
     *
     * @param invocation
     * @param fallback
     * @param invokedParamTypes
     * @return
     * @throws NoSuchMethodException
     */
    private Method findMethod(MethodInvocation invocation, String fallback,
        Class<?>[] invokedParamTypes) throws NoSuchMethodException {
        Class<?> clazz = invocation.getMethod().getDeclaringClass();
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(fallback)) {
                Method temp = methods[i];
                Class<?>[] definedParamTypes = temp.getParameterTypes();
                if (invokedParamTypes.length == definedParamTypes.length) {
                    boolean found = true;
                    for (int j = 0; j < invokedParamTypes.length; j++) {
                        Class<?> invokedParam = invokedParamTypes[j];
                        Class<?> definedParam = definedParamTypes[j];
                        if (!definedParam.isAssignableFrom(invokedParam)) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        return temp;
                    }
                }
            }
        }
        throw new NoSuchMethodException(methodToString(invocation, fallback, invokedParamTypes));
    }

    /**
     * Helper method to get the method name from arguments.
     */
    private String methodToString(MethodInvocation invocation, String methodName,
        Class<?>[] invokedParamTypes) {
        Method method = invocation.getMethod();
        String clazzName = method.getDeclaringClass().getName();
        StringJoiner sj = new StringJoiner(", ", clazzName + "." + methodName + "(", ")");
        if (invokedParamTypes != null) {
            for (int i = 0; i < invokedParamTypes.length; i++) {
                Class<?> c = invokedParamTypes[i];
                sj.add((c == null) ? "null" : c.getName());
            }
        }
        return sj.toString();
    }
}
