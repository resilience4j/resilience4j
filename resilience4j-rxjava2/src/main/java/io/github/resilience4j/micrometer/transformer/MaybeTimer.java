/*
 * Copyright 2023 Mariusz Kopylec
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
package io.github.resilience4j.micrometer.transformer;

import io.github.resilience4j.AbstractMaybeObserver;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.Timer.Context;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;

class MaybeTimer<T> extends Maybe<T> {

    private final Maybe<T> upstream;
    private final Timer timer;

    MaybeTimer(Maybe<T> upstream, Timer timer) {
        this.upstream = upstream;
        this.timer = timer;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> downstream) {
        upstream.subscribe(new TimerMaybeObserver(downstream, timer));
    }

    class TimerMaybeObserver extends AbstractMaybeObserver<T> {

        private final Context context;

        TimerMaybeObserver(MaybeObserver<? super T> downstreamObserver, Timer timer) {
            super(downstreamObserver);
            context = timer.createContext();
        }

        @Override
        protected void hookOnComplete() {
            context.onSuccess();
        }

        @Override
        protected void hookOnError(Throwable e) {
            context.onFailure(e);
        }

        @Override
        protected void hookOnSuccess(T value) {
            context.onSuccess();
        }

        @Override
        protected void hookOnCancel() {
            context.onSuccess();
        }
    }
}
