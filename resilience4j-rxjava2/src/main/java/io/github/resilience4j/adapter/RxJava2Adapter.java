package io.github.resilience4j.adapter;

import io.github.resilience4j.core.EventPublisher;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class RxJava2Adapter {

    /**
     * Converts the EventPublisher into a Flowable.
     *
     * @param eventPublisher the event publisher
     * @param <T>            the type of the event
     * @return the Flowable
     */
    public static <T> Flowable<T> toFlowable(EventPublisher<T> eventPublisher) {
        PublishProcessor<T> publishProcessor = PublishProcessor.create();
        FlowableProcessor<T> flowableProcessor = publishProcessor.toSerialized();
        eventPublisher.onEvent(flowableProcessor::onNext);
        return flowableProcessor;
    }

    /**
     * Converts the EventPublisher into an Observable.
     *
     * @param eventPublisher the event publisher
     * @param <T>            the type of the event
     * @return the Observable
     */
    public static <T> Observable<T> toObservable(EventPublisher<T> eventPublisher) {
        PublishSubject<T> publishSubject = PublishSubject.create();
        Subject<T> serializedSubject = publishSubject.toSerialized();
        eventPublisher.onEvent(serializedSubject::onNext);
        return serializedSubject;
    }
}
