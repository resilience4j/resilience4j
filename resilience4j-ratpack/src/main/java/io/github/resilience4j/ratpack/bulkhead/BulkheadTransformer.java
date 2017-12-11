/*
 * Copyright 2017 Jan Sykora
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
package io.github.resilience4j.ratpack.bulkhead;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import ratpack.exec.Downstream;
import ratpack.exec.Upstream;
import ratpack.func.Function;


public class BulkheadTransformer <T> implements Function<Upstream<? extends T>, Upstream<T>> {

    private final Bulkhead bulkhead;
    private Function<Throwable, ? extends T> recover;

    private BulkheadTransformer(Bulkhead bulkhead) {
        this.bulkhead = bulkhead;
    }

    /**
     * Create a new transformer that can be applied to the {@link ratpack.exec.Promise#transform(Function)} method.
     * The Promised value will pass through the bulkhead, potentially causing it to throw error on reaching
     * limit of concurrent calls.
     *
     * @param bulkhead the bulkhead to use
     * @param <T>         the type of object
     * @return the transformer
     */
    public static <T> BulkheadTransformer<T> of(Bulkhead bulkhead) {
        return new BulkheadTransformer<>(bulkhead);
    }

    /**
     * Set a recovery function that will execute when the rateLimiter limit is exceeded.
     *
     * @param recover the recovery function
     * @return the transformer
     */
    public BulkheadTransformer<T> recover(Function<Throwable, ? extends T> recover) {
        this.recover = recover;
        return this;
    }

    @Override
    public Upstream<T> apply(Upstream<? extends T> upstream) throws Exception {
        return down -> {
            if (bulkhead.isCallPermitted()) {
                // do not allow permits to leak
                upstream.connect(new Downstream<T>() {

                    @Override
                    public void success(T value) {
                        bulkhead.onComplete();
                        down.success(value);
                    }

                    @Override
                    public void error(Throwable throwable) {
                        bulkhead.onComplete();
                        try {
                            if (recover != null) {
                                down.success(recover.apply(throwable));
                            } else {
                                down.error(throwable);
                            }
                        } catch (Throwable t) {
                            down.error(t);
                        }
                    }

                    @Override
                    public void complete() {
                        bulkhead.onComplete();
                        down.complete();
                    }
                });
            } else {
                Throwable t = new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()));
                if (recover != null) {
                    try {
                        down.success(recover.apply(t));
                    } catch (Throwable t2) {
                        down.error(t2);
                    }
                } else {
                    down.error(t);
                }
            }
        };
    }
}
