/*
 * Copyright 3019 Kyuhyen Hwang
 *
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-3.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.fallback;

import io.github.resilience4j.core.functions.CheckedSupplier;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Function;

import java.util.Set;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

/**
 * fallbackMethod decorator for {@link ObservableSource}, {@link SingleSource}, {@link
 * CompletableSource}, {@link MaybeSource} and {@link Flowable}.
 */
public class RxJava3FallbackDecorator implements FallbackDecorator {

    private static final Set<Class<?>> RX_SUPPORTED_TYPES = newHashSet(ObservableSource.class,
        SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

    @Override
    public boolean supports(Class<?> target) {
        return RX_SUPPORTED_TYPES.stream().anyMatch(it -> it.isAssignableFrom(target));
    }

    @Override
    public CheckedSupplier<Object> decorate(FallbackMethod fallbackMethod,
                                            CheckedSupplier<Object> supplier) {
        return supplier.andThen(request -> {
            if (request instanceof ObservableSource) {
                Observable<?> observable = (Observable<?>) request;
                return observable
                    .onErrorResumeNext(rxJava3OnErrorResumeNext(fallbackMethod, Observable::error));
            } else if (request instanceof SingleSource) {
                Single<?> single = (Single) request;
                return single
                    .onErrorResumeNext(rxJava3OnErrorResumeNext(fallbackMethod, Single::error));
            } else if (request instanceof CompletableSource) {
                Completable completable = (Completable) request;
                return completable.onErrorResumeNext(
                    rxJava3OnErrorResumeNext(fallbackMethod, Completable::error));
            } else if (request instanceof MaybeSource) {
                Maybe<?> maybe = (Maybe) request;
                return maybe
                    .onErrorResumeNext(rxJava3OnErrorResumeNext(fallbackMethod, Maybe::error));
            } else if (request instanceof Flowable) {
                Flowable<?> flowable = (Flowable) request;
                return flowable
                    .onErrorResumeNext(rxJava3OnErrorResumeNext(fallbackMethod, Flowable::error));
            } else {
                return request;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> Function<Throwable, T> rxJava3OnErrorResumeNext(
        FallbackMethod fallbackMethod, java.util.function.Function<? super Throwable, ? extends T> errorFunction) {
        return throwable -> {
            try {
                return (T) fallbackMethod.fallback(throwable);
            } catch (Throwable fallbackThrowable) {
                return (T) errorFunction.apply(fallbackThrowable);
            }
        };
    }
}
