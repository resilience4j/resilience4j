package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;

class BulkheadEventProcessor extends EventProcessor<BulkheadEvent> implements
        ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher, EventConsumer<BulkheadEvent> {

    @Override
    public ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher onCallPermitted(
            EventConsumer<BulkheadOnCallPermittedEvent> onCallPermittedEventConsumer) {
        registerConsumer(BulkheadOnCallPermittedEvent.class.getName(),
                onCallPermittedEventConsumer);
        return this;
    }

    @Override
    public ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher onCallRejected(
            EventConsumer<BulkheadOnCallRejectedEvent> onCallRejectedEventConsumer) {
        registerConsumer(BulkheadOnCallRejectedEvent.class.getName(),
                onCallRejectedEventConsumer);
        return this;
    }

    @Override
    public ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher onCallFinished(
            EventConsumer<BulkheadOnCallFinishedEvent> onCallFinishedEventConsumer) {
        registerConsumer(BulkheadOnCallFinishedEvent.class.getName(),
                onCallFinishedEventConsumer);
        return this;
    }

    @Override
    public void consumeEvent(BulkheadEvent event) {
        super.processEvent(event);
    }
}
