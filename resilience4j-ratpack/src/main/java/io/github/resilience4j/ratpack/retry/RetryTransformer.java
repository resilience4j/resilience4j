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
package io.github.resilience4j.ratpack.retry;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.ratpack.internal.AbstractTransformer;
import io.github.resilience4j.retry.Retry;
import ratpack.exec.Downstream;
import ratpack.exec.Upstream;
import ratpack.func.Function;

public class RetryTransformer<T> extends AbstractTransformer<T> {

    private final Retry retry;

    private RetryTransformer(Retry retry) {
        this.retry = retry;
    }

    /**
     * Create a new transformer that can be applied to the {@link ratpack.exec.Promise#transform(Function)} method.
     * The Promised value will pass through the retry, potentially causing it to retry on error.
     *
     * @param retry the retry to use
     * @param <T> the type of object
     * @return the transformer
     */
    public static <T> RetryTransformer<T> of(Retry retry) {
        return new RetryTransformer<>(retry);
    }

    /**
     * Set a recovery function that will execute when the retry limit is exceeded.
     *
     * @param recoverer the recovery function
     * @return the transformer
     */
    public RetryTransformer<T> recover(@Nullable Function<Throwable, ? extends T> recoverer) {
        this.recoverer = recoverer;
        return this;
    }

    @Override
    public Upstream<T> apply(Upstream<? extends T> upstream) {
        return down -> {
            Retry.Context context = retry.context();
            Downstream<T> downstream = new Downstream<T>() {

                @Override
                public void success(T value) {
                    context.onComplete();
                    down.success(value);
                }

                @Override
                public void error(Throwable throwable) {
                    try {
                        context.onError((Exception) throwable);
                        upstream.connect(this);
                    } catch (Exception ex1) {
                        if (recoverer != null) {
                            try {
                                down.success(recoverer.apply(ex1));
                            } catch (Exception ex2) {
                                down.error(ex2);
                            }
                        } else {
                            down.error(ex1);
                        }
                    }
                }

                @Override
                public void complete() {
                    down.complete();
                }
            };
            upstream.connect(downstream);
        };
    }

}
