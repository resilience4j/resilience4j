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

import io.github.resilience4j.AbstractCompletableObserver;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.Timer.Context;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;

class CompletableTimer extends Completable {

    private final Completable upstream;
    private final Timer timer;

    CompletableTimer(Completable upstream, Timer timer) {
        this.upstream = upstream;
        this.timer = timer;
    }

    @Override
    protected void subscribeActual(CompletableObserver downstream) {
        upstream.subscribe(new TimerCompletableObserver(downstream, timer));
    }

    class TimerCompletableObserver extends AbstractCompletableObserver {

        private final Context context;

        TimerCompletableObserver(CompletableObserver downstreamObserver, Timer timer) {
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
        protected void hookOnCancel() {
            context.onSuccess();
        }
    }
}
