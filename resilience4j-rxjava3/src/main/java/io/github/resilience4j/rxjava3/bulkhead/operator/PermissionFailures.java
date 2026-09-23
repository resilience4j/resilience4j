/*
 * Copyright 2026 Oleksandr Shevchenko
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
package io.github.resilience4j.rxjava3.bulkhead.operator;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Extracts the failure of an exceptionally completed permission future.
 */
final class PermissionFailures {

    private PermissionFailures() {
    }

    static Throwable of(CompletableFuture<Void> permission) {
        try {
            permission.join();
            throw new IllegalStateException("Permission future has not failed");
        } catch (CancellationException e) {
            return e;
        } catch (CompletionException e) {
            return unwrap(e);
        }
    }

    /**
     * Unwraps the cause of a {@link CompletionException}, which a Bulkhead implementation can
     * surface when its permission future is a dependent stage.
     */
    static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }
}
