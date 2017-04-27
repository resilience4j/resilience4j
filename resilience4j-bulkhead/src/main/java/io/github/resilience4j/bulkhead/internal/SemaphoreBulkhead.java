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
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.reactivex.Flowable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * A Bulkhead implementation based on a semaphore.
 */
public class SemaphoreBulkhead implements Bulkhead {

    private final String name;
    private final int depth;
    private final Semaphore semaphore;
    private final FlowableProcessor<BulkheadEvent> eventPublisher;

    /**
     * Creates a bulkhead.
     *
     * @param name  the name of the bulkhead
     * @param depth the depth of the bulkhead
     */
    public SemaphoreBulkhead(String name, int depth) {

        this.name = name;
        this.depth = depth;
        this.semaphore = new Semaphore(depth, false);

        // init event publisher
        this.eventPublisher = PublishProcessor.<BulkheadEvent>create()
                                              .toSerialized();
    }

    @Override
    public boolean isCallPermitted() {

        final boolean callPermitted = semaphore.tryAcquire();

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
    public int getConfiguredDepth() {
        return this.depth;
    }

    @Override
    public int getRemainingDepth() {
        return this.semaphore.availablePermits();
    }

    @Override
    public Flowable<BulkheadEvent> getEventStream() {
        return eventPublisher;
    }

    @Override
    public String toString() {
        return String.format("Bulkhead '%s'", this.name);
    }

    private void publishBulkheadEvent(Supplier<BulkheadEvent> eventSupplier) {
        if(eventPublisher.hasSubscribers()) {
            eventPublisher.onNext(eventSupplier.get());
        }
    }

}
