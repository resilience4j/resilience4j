package io.github.resilience4j;

import io.github.resilience4j.fallback.UnhandledFallbackException;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.discovery.exceptions.NoAvailableServiceException;
import io.micronaut.inject.MethodExecutionHandle;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class BaseInterceptor {
     Logger LOG = LoggerFactory.getLogger(BaseInterceptor.class);

    public abstract Optional<? extends MethodExecutionHandle<?, Object>> findFallbackMethod(MethodInvocationContext<Object, Object> context);

    /**
     * Resolves a fallback for the given execution context and exception.
     *
     * @param context   The context
     * @param exception The exception
     * @return Returns the fallback value or throws the original exception
     */
    public Object fallback(MethodInvocationContext<Object, Object> context, RuntimeException exception) {
        if (exception instanceof NoAvailableServiceException) {
            NoAvailableServiceException ex = (NoAvailableServiceException) exception;
            if (LOG.isErrorEnabled()) {
                LOG.debug(ex.getMessage(), ex);
                LOG.error("Type [{}] attempting to resolve fallback for unavailable service [{}]", context.getTarget().getClass().getName(), ex.getServiceID());
            }
        } else {
            if (LOG.isErrorEnabled()) {
                LOG.error("Type [" + context.getTarget().getClass().getName() + "] executed with error: " + exception.getMessage(), exception);
            }
        }
        Optional<? extends MethodExecutionHandle<?, Object>> fallback = findFallbackMethod(context);
        if (fallback.isPresent()) {
            MethodExecutionHandle<?, Object> fallbackMethod = fallback.get();
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Type [{}] resolved fallback: {}", context.getTarget().getClass().getName(), fallbackMethod);
                }
                return fallbackMethod.invoke(context.getParameterValues());
            } catch (Exception e) {
                throw new UnhandledFallbackException("Error invoking fallback for type [" + context.getTarget().getClass().getName() + "]: " + e.getMessage(), e);
            }
        } else {
            throw exception;
        }
    }

    public <T> CompletableFuture<Object> fallbackCompletable(CompletionStage<T> stage, MethodInvocationContext<Object, Object> context) {
        CompletableFuture<Object> newFuture = new CompletableFuture<>();
        stage.whenComplete((o, throwable) -> {
            if (throwable == null) {
                newFuture.complete(o);
            } else {
                Optional<? extends MethodExecutionHandle<?, Object>> fallbackMethod = findFallbackMethod(context);
                if (fallbackMethod.isPresent()) {
                    MethodExecutionHandle<?, Object> fallbackHandle = fallbackMethod.get();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Type [{}] resolved fallback: {}", context.getTarget().getClass(), fallbackHandle);
                    }
                    try {
                        CompletableFuture<Object> resultingFuture = (CompletableFuture<Object>) fallbackHandle.invoke(context.getParameterValues());
                        if (resultingFuture == null) {
                            newFuture.completeExceptionally(new UnhandledFallbackException("Fallback handler [" + fallbackHandle + "] returned null value"));
                        } else {
                            resultingFuture.whenComplete((o1, throwable1) -> {
                                if (throwable1 == null) {
                                    newFuture.complete(o1);
                                } else {
                                    newFuture.completeExceptionally(throwable1);
                                }
                            });
                        }
                    } catch (Exception e) {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("Error invoking Fallback [" + fallbackHandle + "]: " + e.getMessage(), e);
                        }
                        newFuture.completeExceptionally(throwable);
                    }
                } else {
                    newFuture.completeExceptionally(throwable);
                }
            }
        });
        return newFuture;
    }

    public Flowable<Object> fallbackFlowable(Flowable<Object> flowable, MethodInvocationContext<Object, Object> context) {
        flowable.onErrorResumeNext(throwable -> {
            Optional<? extends MethodExecutionHandle<?, Object>> fallbackMethod = findFallbackMethod(context);
            if (fallbackMethod.isPresent()) {
                MethodExecutionHandle<?, Object> fallbackHandle = fallbackMethod.get();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Type [{}] resolved fallback: {}", context.getTarget().getClass(), fallbackHandle);
                }
                Object fallbackResult;
                try {
                    fallbackResult = fallbackHandle.invoke(context.getParameterValues());
                } catch (Exception e) {
                    return Flowable.error(throwable);
                }
                if (fallbackResult == null) {
                    return Flowable.error(new UnhandledFallbackException("Fallback handler [" + fallbackHandle + "] returned null value"));
                } else {
                    return ConversionService.SHARED.convert(fallbackResult, Publisher.class)
                        .orElseThrow(() -> new UnhandledFallbackException("Unsupported Reactive type: " + fallbackResult));
                }
            }
            return Flowable.error(throwable);
        });
        return flowable;
    }



}
