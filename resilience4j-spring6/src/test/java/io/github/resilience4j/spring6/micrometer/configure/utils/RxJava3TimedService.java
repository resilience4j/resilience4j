package io.github.resilience4j.spring6.micrometer.configure.utils;

import io.github.resilience4j.micrometer.annotation.Timer;
import io.reactivex.rxjava3.core.*;
import org.springframework.stereotype.Component;

@Component
public class RxJava3TimedService {

    public static final String COMPLETABLE_TIMER_NAME = "Completable";
    public static final String SINGLE_TIMER_NAME = "Single";
    public static final String MAYBE_TIMER_NAME = "Maybe";
    public static final String OBSERVABLE_TIMER_NAME = "Observable";
    public static final String FLOWABLE_TIMER_NAME = "Flowable";

    @Timer(name = COMPLETABLE_TIMER_NAME)
    public Completable succeedCompletable() {
        return Completable.complete();
    }

    @Timer(name = COMPLETABLE_TIMER_NAME)
    public Completable failCompletable() {
        return Completable.error(new IllegalStateException());
    }

    @Timer(name = COMPLETABLE_TIMER_NAME, fallbackMethod = "fallbackCompletable")
    public Completable recoverCompletable() {
        return Completable.error(new IllegalStateException());
    }

    @Timer(name = "#root.args[0]", fallbackMethod = "${missing.property:fallbackCompletable}")
    public Completable recoverCompletable(String timerName) {
        return Completable.error(new IllegalStateException());
    }

    @Timer(name = SINGLE_TIMER_NAME)
    public Single<String> succeedSingle(int number) {
        return Single.just(String.valueOf(number));
    }

    @Timer(name = SINGLE_TIMER_NAME)
    public Single<Void> failSingle() {
        return Single.error(new IllegalStateException());
    }

    @Timer(name = SINGLE_TIMER_NAME, fallbackMethod = "fallbackSingle")
    public Single<String> recoverSingle(int number) {
        return Single.error(new IllegalStateException("Single recovered " + number));
    }

    @Timer(name = "#root.args[0]", fallbackMethod = "${missing.property:fallbackSingle}")
    public Single<String> recoverSingle(String timerName, int number) {
        return Single.error(new IllegalStateException("Single recovered " + number));
    }

    @Timer(name = MAYBE_TIMER_NAME)
    public Maybe<String> succeedMaybe(int number) {
        return Maybe.just(String.valueOf(number));
    }

    @Timer(name = MAYBE_TIMER_NAME)
    public Maybe<Void> failMaybe() {
        return Maybe.error(new IllegalStateException());
    }

    @Timer(name = MAYBE_TIMER_NAME, fallbackMethod = "fallbackMaybe")
    public Maybe<String> recoverMaybe(int number) {
        return Maybe.error(new IllegalStateException("Maybe recovered " + number));
    }

    @Timer(name = "#root.args[0]", fallbackMethod = "${missing.property:fallbackMaybe}")
    public Maybe<String> recoverMaybe(String timerName, int number) {
        return Maybe.error(new IllegalStateException("Maybe recovered " + number));
    }

    @Timer(name = OBSERVABLE_TIMER_NAME)
    public Observable<String> succeedObservable(int number) {
        return Observable.just(String.valueOf(number));
    }

    @Timer(name = OBSERVABLE_TIMER_NAME)
    public Observable<Void> failObservable() {
        return Observable.error(new IllegalStateException());
    }

    @Timer(name = OBSERVABLE_TIMER_NAME, fallbackMethod = "fallbackObservable")
    public Observable<String> recoverObservable(int number) {
        return Observable.error(new IllegalStateException("Observable recovered " + number));
    }

    @Timer(name = "#root.args[0]", fallbackMethod = "${missing.property:fallbackObservable}")
    public Observable<String> recoverObservable(String timerName, int number) {
        return Observable.error(new IllegalStateException("Observable recovered " + number));
    }

    @Timer(name = FLOWABLE_TIMER_NAME)
    public Flowable<String> succeedFlowable(int number) {
        return Flowable.just(String.valueOf(number));
    }

    @Timer(name = FLOWABLE_TIMER_NAME)
    public Flowable<Void> failFlowable() {
        return Flowable.error(new IllegalStateException());
    }

    @Timer(name = FLOWABLE_TIMER_NAME, fallbackMethod = "fallbackFlowable")
    public Flowable<String> recoverFlowable(int number) {
        return Flowable.error(new IllegalStateException("Flowable recovered " + number));
    }

    @Timer(name = "#root.args[0]", fallbackMethod = "${missing.property:fallbackFlowable}")
    public Flowable<String> recoverFlowable(String timerName, int number) {
        return Flowable.error(new IllegalStateException("Flowable recovered " + number));
    }

    private Completable fallbackCompletable(IllegalStateException e) {
        return Completable.complete();
    }

    private Single<String> fallbackSingle(IllegalStateException e) {
        return Single.just(e.getMessage());
    }

    private Maybe<String> fallbackMaybe(IllegalStateException e) {
        return Maybe.just(e.getMessage());
    }

    private Observable<String> fallbackObservable(IllegalStateException e) {
        return Observable.just(e.getMessage());
    }

    private Flowable<String> fallbackFlowable(IllegalStateException e) {
        return Flowable.just(e.getMessage());
    }
}
