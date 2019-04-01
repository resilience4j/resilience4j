package io.github.resilience4j.utils;

import io.github.resilience4j.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.recovery.RecoveryFunction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class RecoveryFunctionUtils {

    /**
     * create new instance of {@link RecoveryFunction} class. If the class is {@link DefaultRecoveryFunction}, then returns its singleton instance.
     */
    @SuppressWarnings("unchecked")
    public static <T extends RecoveryFunction<?>> T getInstance(Class<T> clazz) {
        try {
            return DefaultRecoveryFunction.class.isAssignableFrom(clazz) ? (T) DefaultRecoveryFunction.getInstance() : clazz.newInstance();
        } catch (Throwable throwable) {
            throw new RuntimeException("can't create a recovery function", throwable);
        }
    }

    /**
     * decorate {@link CompletionStage} with {@link RecoveryFunction}. {@link CompletionStage} will be recovered from exceptional completion by {@link RecoveryFunction}.
     * @param recoveryFunctionSupplier {@link RecoveryFunction} supplier
     * @param completionStage target {@link CompletionStage}
     * @param <T> return type of {@link CompletionStage}
     * @return {@link RecoveryFunction} decorated {@link CompletionStage}
     */
    public static <T> CompletionStage<T> decorateCompletionStage(Supplier<? extends RecoveryFunction<T>> recoveryFunctionSupplier, CompletionStage<T> completionStage) {
        CompletableFuture<T> promise = new CompletableFuture<>();

        completionStage.whenComplete((result, throwable) -> {
           if (throwable != null) {
                try {
                    RecoveryFunction<T> recovery = recoveryFunctionSupplier.get();
                    promise.complete(recovery.apply(throwable));
                } catch (Throwable recoveryThrowable) {
                    promise.completeExceptionally(recoveryThrowable);
                }
            } else {
               promise.complete(result);
           }
        });

        return promise;
    }
}
