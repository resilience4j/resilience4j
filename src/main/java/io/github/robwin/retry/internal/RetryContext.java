/*
 *
 *  Copyright 2016 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.robwin.retry.internal;

import io.github.robwin.retry.Retry;
import io.github.robwin.retry.RetryConfig;
import io.github.robwin.retry.event.RetryEvent;
import io.github.robwin.retry.event.RetryOnErrorEvent;
import io.github.robwin.retry.event.RetryOnSuccessEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import javaslang.collection.Stream;
import javaslang.control.Option;
import javaslang.control.Try;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RetryContext implements Retry {

    private final AtomicInteger numOfAttempts = new AtomicInteger(0);
    private AtomicReference<Exception> lastException = new AtomicReference<>();
    private AtomicReference<RuntimeException> lastRuntimeException = new AtomicReference<>();

    private final FlowableProcessor<RetryEvent> eventPublisher;

    private String id;
    private int maxAttempts;
    private Function<Integer, Long> intervalFunction;
    private Predicate<Throwable> exceptionPredicate;
    /*package*/ static Try.CheckedConsumer<Long> sleepFunction = Thread::sleep;

    public RetryContext(String id, RetryConfig config){
        this.id = id;
        this.maxAttempts = config.getMaxAttempts();
        this.intervalFunction = config.getIntervalFunction();
        this.exceptionPredicate = config.getExceptionPredicate();
        PublishProcessor<RetryEvent> publisher = PublishProcessor.create();
        this.eventPublisher = publisher.toSerialized();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void onSuccess() {
        int currentNumOfAttempts = numOfAttempts.get();
        if(currentNumOfAttempts > 0){
            Throwable throwable = Option.of(lastException.get()).getOrElse(lastRuntimeException.get());
            publishRetryEvent(() -> new RetryOnSuccessEvent(getId(), currentNumOfAttempts, throwable));
        }
    }

    private void throwOrSleepAfterException() throws Exception {
        int currentNumOfAttempts = numOfAttempts.incrementAndGet();
        if(currentNumOfAttempts >= maxAttempts){
            Exception throwable = lastException.get();
            publishRetryEvent(() -> new RetryOnErrorEvent(getId(), currentNumOfAttempts, throwable));
            throw throwable;
        }else{
            waitIntervalAfterFailure();
        }
    }

    public void throwOrSleepAfterRuntimeException(){
        int currentNumOfAttempts = numOfAttempts.incrementAndGet();
        if(currentNumOfAttempts >= maxAttempts){
            RuntimeException throwable = lastRuntimeException.get();
            publishRetryEvent(() -> new RetryOnErrorEvent(getId(), currentNumOfAttempts, throwable));
            throw throwable;
        }else{
            waitIntervalAfterFailure();
        }
    }

    private void waitIntervalAfterFailure() {
        // wait interval until the next attempt should start
        long interval = intervalFunction.apply(numOfAttempts.get());
        Try.run(() -> sleepFunction.accept(interval))
            .getOrElseThrow(ex -> lastRuntimeException.get());
    }

    @Override
    public void onError(Exception exception) throws Throwable{
        if(exceptionPredicate.test(exception)){
            lastException.set(exception);
            throwOrSleepAfterException();
        }else{
            throw exception;
        }
    }

    @Override
    public void onRuntimeError(RuntimeException runtimeException){
        if(exceptionPredicate.test(runtimeException)){
            lastRuntimeException.set(runtimeException);
            throwOrSleepAfterRuntimeException();
        }else{
            throw runtimeException;
        }
    }

    private void publishRetryEvent(Supplier<RetryEvent> event) {
        if(eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(event.get());
        }
    }

    @Override
    public Flowable<RetryEvent> getEventStream() {
        return eventPublisher;
    }
}
