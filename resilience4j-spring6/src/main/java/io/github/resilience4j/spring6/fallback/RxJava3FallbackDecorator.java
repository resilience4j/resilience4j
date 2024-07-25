package io.github.resilience4j.spring6.fallback;

import io.github.resilience4j.core.functions.CheckedSupplier;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Function;

import java.util.Set;

import static io.github.resilience4j.spring6.utils.AspectUtil.newHashSet;

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
