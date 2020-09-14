/*
 * Copyright 2020 Michael Pollind
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
package io.github.resilience4j.micronaut

import io.micronaut.context.annotation.Executable

import java.util.concurrent.CompletableFuture

abstract class TestDummyService {
    CompletableFuture<String> asyncError() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Test"));
        return future
    }

    String syncError() {
        throw new RuntimeException("Test");
    }

    @Executable
    CompletableFuture<String> completionStageRecovery() {
        return CompletableFuture.supplyAsync({ -> 'recovered' });
    }

    @Executable
    CompletableFuture<String> completionStageRecoveryParam(String parameter) {
        return CompletableFuture.supplyAsync({ -> parameter });
    }

    @Executable
    String syncRecovery() {
        return "recovered"
    }

    @Executable
    String syncRecoveryParam(String parameter) {
        return parameter
    }
}
