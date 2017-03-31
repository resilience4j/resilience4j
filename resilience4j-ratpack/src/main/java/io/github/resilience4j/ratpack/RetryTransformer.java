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
package io.github.resilience4j.ratpack;

import io.github.resilience4j.retry.Retry;
import ratpack.exec.Downstream;
import ratpack.exec.Upstream;
import ratpack.func.Function;

public class RetryTransformer<T> implements Function<Upstream<? extends T>, Upstream<T>> {

    private final Retry retry;
    private Function<Throwable, ? extends T> recoverer;

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
    public RetryTransformer<T> recover(Function<Throwable, ? extends T> recoverer) {
        this.recoverer = recoverer;
        return this;
    }

    @Override
    public Upstream<T> apply(Upstream<? extends T> upstream) throws Exception {
        return down -> {
            Downstream<T> downstream = new Downstream<T>() {

                @Override
                public void success(T value) {
                    retry.onSuccess();
                    down.success(value);
                }

                @Override
                public void error(Throwable throwable) {
                    try {
                        retry.onError((Exception) throwable);
                        upstream.connect(this);
                    } catch (Throwable t) {
                        if (recoverer != null) {
                            try {
                                down.success(recoverer.apply(t));
                            } catch (Throwable t2) {
                                down.error(t2);
                            }
                        } else {
                            down.error(t);
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
