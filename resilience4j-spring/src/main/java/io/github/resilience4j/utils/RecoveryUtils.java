package io.github.resilience4j.utils;

import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

//TODO check return type, improve code
public class RecoveryUtils {
    public static Object invoke(String recoveryMethodName, Object[] args, Throwable throwable, Object target) throws Throwable {
        if (recoveryMethodName.isEmpty()) {
            throw throwable;
        }

        Class[] params = new Class[args.length + 1];

        for (int i = 0; i < args.length; i++) {
            params[i] = args[i].getClass();
        }

        params[args.length] = Throwable.class;

        Method recovery = ReflectionUtils.findMethod(target.getClass(), recoveryMethodName, params);

        if (args.length != 0) {
            Object[] newArgs = Arrays.copyOf(args, args.length + 1);
            newArgs[args.length] = throwable;

            return recovery.invoke(target, newArgs);
        } else {
            return recovery.invoke(target, throwable);
        }

    }

    public static <T> CompletionStage<T> decorateCompletionStage(String recoveryMethodName, Object[] args, Object target, CompletionStage<T> completionStage) {
        CompletableFuture<T> promise = new CompletableFuture<>();

        completionStage.whenComplete((result, throwable) -> {
            if (throwable != null) {
                try {
                    promise.complete((T) invoke(recoveryMethodName, args, throwable, target));
                } catch (Throwable recoveryThrowable) {
                    promise.completeExceptionally(recoveryThrowable);
                }
            } else {
                promise.complete(result);
            }
        });

        return promise;
    }

    public static <T> Function<? super Throwable, ? extends Publisher<? extends T>> reactorOnErrorResume(String recoveryMethodName, Object[] args, Object target, Function<? super Throwable, ? extends Publisher<? extends T>> errorFunction) {
        return (throwable) -> {
            try {
                return (Publisher<? extends T>) invoke(recoveryMethodName, args, throwable, target);
            } catch (Throwable recoverThrowable) {
                return errorFunction.apply(recoverThrowable);
            }
        };
    }

    public static <T> io.reactivex.functions.Function<Throwable, T> rxJava2OnErrorResumeNext(String recoveryMethodName, Object[] args, Object target, Function<? super Throwable, ? extends T> errorFunction) {
        return (throwable) -> {
            try {
                return (T) invoke(recoveryMethodName, args, throwable, target);
            } catch (Throwable recoverThrowable) {
                return (T) errorFunction.apply(recoverThrowable);
            }
        };
    }
}
