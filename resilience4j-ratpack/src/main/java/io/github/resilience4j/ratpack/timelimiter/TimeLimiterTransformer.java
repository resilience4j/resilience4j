/*
 * Copyright 2017 Dan Maas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.ratpack.timelimiter;

import io.github.resilience4j.ratpack.internal.AbstractTransformer;
import io.github.resilience4j.timelimiter.TimeLimiter;
import ratpack.exec.*;
import ratpack.func.Function;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimeLimiterTransformer<T> extends AbstractTransformer<T> {

    private final TimeLimiter timeLimiter;

    private TimeLimiterTransformer(TimeLimiter timeLimiter) {
        this.timeLimiter = timeLimiter;
    }

    /**
     * Create a new transformer that can be applied to the {@link ratpack.exec.Promise#transform(Function)}
     * method. The Promised value will pass through the timeLimiter, potentially causing it to time out.
     *
     * @param timeLimiter the timeLimiter to use
     * @param <T>         the type of object
     * @return the transformer
     */
    public static <T> TimeLimiterTransformer<T> of(TimeLimiter timeLimiter) {
        return new TimeLimiterTransformer<>(timeLimiter);
    }

    /**
     * Set a recovery function that will execute when the timeLimiter timeout is exceeded.
     *
     * @param recoverer the recovery function
     * @return the transformer
     */
    public TimeLimiterTransformer<T> recover(Function<Throwable, ? extends T> recoverer) {
        this.recoverer = recoverer;
        return this;
    }

    @Override
    public Upstream<T> apply(Upstream<? extends T> upstream) throws Exception {
        return down -> {
            Promise<? extends T> promise = Promise.async(upstream);
            ScheduledExecutorService scheduler = Execution.current().getController().getExecutor();
            AtomicBoolean done = new AtomicBoolean(false);
            Promise<? extends T> timedPromise = Promise.async(innerDown -> {
                ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> scheduleTimeout(done, innerDown),
                    timeLimiter.getTimeLimiterConfig().getTimeoutDuration().toMillis(),
                    TimeUnit.MILLISECONDS);
                Execution.fork().start(execution ->
                    promise.result(execResult -> onPromiseResult(done, innerDown, timeoutFuture, execResult))
                );
            });
            timedPromise.result(execResult -> onTimedPromiseResult(down, execResult));
        };
    }

    public void scheduleTimeout(AtomicBoolean done, Downstream<? super T> innerDownstream) {
        if (!done.getAndSet(true)) {
            Throwable t = new TimeoutException();
            if (recoverer != null) {
                try {
                    innerDownstream.success(recoverer.apply(t));
                } catch (Throwable t2) {
                    innerDownstream.error(t2);
                }
            } else {
                innerDownstream.error(t);
            }
        }
    }

    public void onPromiseResult(AtomicBoolean done,
                                Downstream<? super T> innerDownstream,
                                ScheduledFuture<?> timeoutFuture,
                                ExecResult<? extends T> execResult) {
        if (!done.getAndSet(true)) {
            if (!timeoutFuture.isDone()) {
                timeoutFuture.cancel(false);
            }
            if (execResult.getThrowable() != null) {
                innerDownstream.error(execResult.getThrowable());
            }
            if (execResult.getValue() != null) {
                innerDownstream.success(execResult.getValue());
            }
        }
    }

    public void onTimedPromiseResult(Downstream<? super T> downstream, ExecResult<? extends T> execResult) {
        T result = execResult.getValue();
        Throwable throwable = execResult.getThrowable();
        if (result != null) {
            timeLimiter.onSuccess();
            downstream.success(result);
        }
        onException(downstream, throwable);
    }

    public void onException(Downstream<? super T> downstream, Throwable throwable) {
        if (throwable != null) {
            Throwable cause;
            if (throwable instanceof CompletionException) {
                cause = throwable.getCause();
            } else if (throwable instanceof ExecutionException) {
                cause = throwable.getCause();
                if (cause == null) {
                    cause = throwable;
                }
            } else {
                cause = throwable;
            }
            timeLimiter.onError(cause);
            downstream.error(cause);
        }
    }

}
