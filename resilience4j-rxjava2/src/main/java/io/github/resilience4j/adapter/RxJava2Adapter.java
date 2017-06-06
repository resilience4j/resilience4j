package io.github.resilience4j.adapter;

import io.github.resilience4j.core.EventPublisher;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;

public class RxJava2Adapter {

    public static <T> Flowable<T> toFlowable(EventPublisher<T> eventPublisher) {
        PublishProcessor<T> publishProcessor = PublishProcessor.create();
        FlowableProcessor<T> flowableProcessor = publishProcessor.toSerialized();
        eventPublisher.onEvent(flowableProcessor::onNext);
        return flowableProcessor;
    }
}
