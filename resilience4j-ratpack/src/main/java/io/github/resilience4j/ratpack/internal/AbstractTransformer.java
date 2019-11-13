/*
 * Copyright 2018 Dan Maas
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

package io.github.resilience4j.ratpack.internal;

import io.github.resilience4j.core.lang.Nullable;
import ratpack.exec.Downstream;
import ratpack.exec.Promise;
import ratpack.exec.Upstream;
import ratpack.func.Function;

public abstract class AbstractTransformer<T> implements
    Function<Upstream<? extends T>, Upstream<T>> {

    @Nullable
    protected Function<Throwable, ? extends T> recoverer;

    protected void handleRecovery(Downstream<? super T> down, Throwable throwable) {
        try {
            if (recoverer != null) {
                T result = recoverer.apply(throwable);
                if (result instanceof Promise) {
                    ((Promise) result)
                        .onError((t) -> down.error((Throwable) t))
                        .then((r) -> down.success((T) r));
                } else {
                    down.success(result);
                }
            } else {
                down.error(throwable);
            }
        } catch (Exception ex) {
            down.error(ex);
        }
    }
}
