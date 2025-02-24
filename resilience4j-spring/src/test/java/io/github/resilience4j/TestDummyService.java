/*
 * Copyright 2019 lespinsideg
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

import io.reactivex.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface TestDummyService {

    String BACKEND = "backendA";
    String BACKEND_B = "backendB";

    String sync();

    String syncSuccess();

    CompletionStage<String> asyncThreadPool();

    CompletionStage<String> asyncThreadPoolSuccess();

    CompletionStage<String> async();

    Flux<String> flux();

    Mono<String> mono(String parameter);

    Observable<String> observable();

    Single<String> single();

    Completable completable();

    Maybe<String> maybe();

    Flowable<String> flowable();

    io.reactivex.rxjava3.core.Observable<String> rx3Observable();

    io.reactivex.rxjava3.core.Single<String> rx3Single();

    io.reactivex.rxjava3.core.Completable rx3Completable();

    io.reactivex.rxjava3.core.Maybe<String> rx3Maybe();

    io.reactivex.rxjava3.core.Flowable<String> rx3Flowable();

    default String spelSync(String backend) {
        return syncError();
    }

    default Mono<String> spelMono(String backend) {
        return monoError(backend);
    }

    default String syncError() {
        throw new RuntimeException("Test");
    }

    default CompletionStage<String> asyncError() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Test"));

        return future;
    }

    default Flux<String> fluxError() {
        return Flux.error(new RuntimeException("Test"));
    }

    default Mono<String> monoError(String parameter) {
        return Mono.error(new RuntimeException("Test"));
    }

    default Observable<String> observableError() {
        return Observable.error(new RuntimeException("Test"));
    }

    default Single<String> singleError() {
        return Single.error(new RuntimeException("Test"));
    }

    default Completable completableError() {
        return Completable.error(new RuntimeException("Test"));
    }

    default Maybe<String> maybeError() {
        return Maybe.error(new RuntimeException("Test"));
    }

    default Flowable<String> flowableError() {
        return Flowable.error(new RuntimeException("Test"));
    }

    default io.reactivex.rxjava3.core.Observable<String> rx3ObservableError() {
        return io.reactivex.rxjava3.core.Observable.error(new RuntimeException("Test"));
    }

    default io.reactivex.rxjava3.core.Single<String> rx3SingleError() {
        return io.reactivex.rxjava3.core.Single.error(new RuntimeException("Test"));
    }

    default io.reactivex.rxjava3.core.Completable rx3CompletableError() {
        return io.reactivex.rxjava3.core.Completable.error(new RuntimeException("Test"));
    }

    default io.reactivex.rxjava3.core.Maybe<String> rx3MaybeError() {
        return io.reactivex.rxjava3.core.Maybe.error(new RuntimeException("Test"));
    }

    default io.reactivex.rxjava3.core.Flowable<String> rx3FlowableError() {
        return io.reactivex.rxjava3.core.Flowable.error(new RuntimeException("Test"));
    }

    default String recovery(RuntimeException throwable) {
        return "recovered";
    }

    default CompletionStage<String> completionStageRecovery(Throwable throwable) {
        return CompletableFuture.supplyAsync(() -> "recovered");
    }

    default Flux<String> fluxRecovery(Throwable throwable) {
        return Flux.just("recovered");
    }

    default Mono<String> monoRecovery(String parameter, Throwable throwable) {
        return Mono.just(parameter);
    }

    default Observable<String> observableRecovery(Throwable throwable) {
        return Observable.just("recovered");
    }

    default Single<String> singleRecovery(Throwable throwable) {
        return Single.just("recovered");
    }

    default Completable completableRecovery(Throwable throwable) {
        return Completable.complete();
    }

    default Maybe<String> maybeRecovery(Throwable throwable) {
        return Maybe.just("recovered");
    }

    default Flowable<String> flowableRecovery(Throwable throwable) {
        return Flowable.just("recovered");
    }

    default io.reactivex.rxjava3.core.Observable<String> rx3ObservableRecovery(Throwable throwable) {
        return io.reactivex.rxjava3.core.Observable.just("recovered");
    }

    default io.reactivex.rxjava3.core.Single<String> rx3SingleRecovery(Throwable throwable) {
        return io.reactivex.rxjava3.core.Single.just("recovered");
    }

    default io.reactivex.rxjava3.core.Completable rx3CompletableRecovery(Throwable throwable) {
        return io.reactivex.rxjava3.core.Completable.complete();
    }

    default io.reactivex.rxjava3.core.Maybe<String> rx3MaybeRecovery(Throwable throwable) {
        return io.reactivex.rxjava3.core.Maybe.just("recovered");
    }

    default io.reactivex.rxjava3.core.Flowable<String> rx3FlowableRecovery(Throwable throwable) {
        return io.reactivex.rxjava3.core.Flowable.just("recovered");
    }
}
