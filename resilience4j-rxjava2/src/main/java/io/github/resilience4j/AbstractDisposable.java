/*
 * Copyright 2019 Robert Winkler
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
package io.github.resilience4j;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractDisposable implements Disposable {

    private final AtomicReference<Disposable> subscription = new AtomicReference<>();

    public void onSubscribe(Disposable disposable) {
        if (DisposableHelper.setOnce(this.subscription, disposable)) {
            hookOnSubscribe();
        }
    }

    protected abstract void hookOnSubscribe();

    @Override
    public void dispose() {
        if (DisposableHelper.dispose(subscription)) {
            hookOnCancel();
        }
    }

    void whenNotDisposed(Runnable runnable) {
        if (!isDisposed()) {
            runnable.run();
        }
    }

    void whenNotCompleted(Runnable runnable) {
        if (DisposableHelper.dispose(subscription)) {
            runnable.run();
        }
    }

    protected abstract void hookOnCancel();

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(subscription.get());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
