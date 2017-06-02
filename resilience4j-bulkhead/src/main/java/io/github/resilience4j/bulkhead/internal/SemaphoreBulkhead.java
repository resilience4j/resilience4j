/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead.internal;


import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.vavr.Lazy;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A Bulkhead implementation based on a semaphore.
 */
public class SemaphoreBulkhead implements Bulkhead{

    private final String name;
    private final Semaphore semaphore;
    private final BulkheadConfig bulkheadConfig;
    private final FlowableProcessor<BulkheadEvent> eventPublisher;
    private final BulkheadMetrics metrics;
    private final Lazy<EventPublisher> lazyEventConsumer;

    /**
     * Creates a bulkhead using a configuration supplied
     *
     * @param name the name of this bulkhead
     * @param bulkheadConfig custom bulkhead configuration
     */
    public SemaphoreBulkhead(String name, BulkheadConfig bulkheadConfig) {
        this.name = name;
        this.bulkheadConfig = bulkheadConfig != null ? bulkheadConfig
                                                     : BulkheadConfig.ofDefaults();
        // init semaphore
        this.semaphore = new Semaphore(this.bulkheadConfig.getMaxConcurrentCalls(), true);

        // init event publisher
        this.eventPublisher = PublishProcessor.<BulkheadEvent>create()
                                              .toSerialized();
        this.lazyEventConsumer = Lazy.of(() -> new EventDispatcher(getEventStream()));

        this.metrics = new BulkheadMetrics();
    }

    /**
     * Creates a bulkhead with a default config.
     *
     * @param name the name of this bulkhead
     */
    public SemaphoreBulkhead(String name) {
        this(name, BulkheadConfig.ofDefaults());
    }

    /**
     * Create a bulkhead using a configuration supplier
     *
     * @param name the name of this bulkhead
     * @param configSupplier BulkheadConfig supplier
     */
    public SemaphoreBulkhead(String name, Supplier<BulkheadConfig> configSupplier) {
        this(name, configSupplier.get());
    }


    @Override
    public boolean isCallPermitted() {

        boolean callPermitted = tryEnterBulkhead();

        publishBulkheadEvent(
            () -> callPermitted ? new BulkheadOnCallPermittedEvent(name)
                                : new BulkheadOnCallRejectedEvent(name)
        );

        return callPermitted;
    }

    @Override
    public void onComplete() {
        semaphore.release();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public BulkheadConfig getBulkheadConfig() {
        return bulkheadConfig;
    }

    @Override
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public Flowable<BulkheadEvent> getEventStream() {
        return eventPublisher;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return lazyEventConsumer.get();
    }

    private class EventDispatcher implements EventPublisher, io.reactivex.functions.Consumer<BulkheadEvent> {

        private volatile Consumer<BulkheadOnCallPermittedEvent> onCallPermittedEventConsumer;
        private volatile Consumer<BulkheadOnCallRejectedEvent> onCallRejectedEventConsumer;

        EventDispatcher(Flowable<BulkheadEvent> eventStream) {
            eventStream.subscribe(this);
        }

        @Override
        public EventPublisher onCallPermitted(Consumer<BulkheadOnCallPermittedEvent> onCallPermittedEventConsumer) {
            this.onCallPermittedEventConsumer = onCallPermittedEventConsumer;
            return this;
        }

        @Override
        public EventPublisher onCallRejected(Consumer<BulkheadOnCallRejectedEvent> onCallRejectedEventConsumer) {
            this.onCallRejectedEventConsumer = onCallRejectedEventConsumer;
            return this;
        }

        @Override
        public void accept(BulkheadEvent event) throws Exception {
            BulkheadEvent.Type eventType = event.getEventType();
            switch (eventType) {
                case CALL_PERMITTED:
                    if(onCallPermittedEventConsumer != null){
                        onCallPermittedEventConsumer.accept((BulkheadOnCallPermittedEvent) event);
                    }
                    break;
                case CALL_REJECTED:
                    if(onCallRejectedEventConsumer != null) {
                        onCallRejectedEventConsumer.accept((BulkheadOnCallRejectedEvent) event);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Bulkhead '%s'", this.name);
    }

    boolean tryEnterBulkhead() {

        boolean callPermitted = false;
        long timeout = bulkheadConfig.getMaxWaitTime();

        if (timeout == 0) {
            callPermitted = semaphore.tryAcquire();
        }
        else {
            try {
                callPermitted = semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ex) {
                callPermitted = false;
            }
        }
        //
        return callPermitted;
    }

    private void publishBulkheadEvent(Supplier<BulkheadEvent> eventSupplier) {
        if(eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(eventSupplier.get());
        }
    }

    private final class BulkheadMetrics implements Metrics {
        private BulkheadMetrics() {
        }

        @Override
        public int getAvailableConcurrentCalls() {
            return semaphore.availablePermits();
        }
    }

}
