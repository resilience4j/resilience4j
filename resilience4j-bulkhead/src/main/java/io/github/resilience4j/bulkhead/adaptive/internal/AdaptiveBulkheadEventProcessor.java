package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.event.*;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.EventPublisher;

class AdaptiveBulkheadEventProcessor extends EventProcessor<AdaptiveBulkheadEvent> implements
    AdaptiveBulkhead.AdaptiveEventPublisher, EventConsumer<AdaptiveBulkheadEvent> {

    @Override
    public EventPublisher<?> onLimitIncreased(
        EventConsumer<BulkheadOnLimitIncreasedEvent> eventConsumer) {
        registerConsumer(BulkheadOnLimitIncreasedEvent.class.getName(), eventConsumer);
        return this;
    }

    @Override
    public EventPublisher<?> onLimitDecreased(
        EventConsumer<BulkheadOnLimitDecreasedEvent> eventConsumer) {
        registerConsumer(BulkheadOnLimitDecreasedEvent.class.getName(), eventConsumer);
        return this;
    }

    @Override
    public EventPublisher<?> onSuccess(EventConsumer<BulkheadOnSuccessEvent> eventConsumer) {
        registerConsumer(BulkheadOnSuccessEvent.class.getName(), eventConsumer);
        return this;
    }

    @Override
    public EventPublisher<?> onError(EventConsumer<BulkheadOnErrorEvent> eventConsumer) {
        registerConsumer(BulkheadOnErrorEvent.class.getName(), eventConsumer);
        return this;
    }

    @Override
    public EventPublisher<?> onIgnoredError(
        EventConsumer<BulkheadOnIgnoreEvent> eventConsumer) {
        registerConsumer(BulkheadOnIgnoreEvent.class.getName(), eventConsumer);
        return this;
    }

    @Override
    public EventPublisher<?> onStateTransition(
        EventConsumer<BulkheadOnStateTransitionEvent> eventConsumer) {
        registerConsumer(BulkheadOnStateTransitionEvent.class.getName(), eventConsumer);
        return this;
    }

    @Override
    public void consumeEvent(AdaptiveBulkheadEvent event) {
        super.processEvent(event);
    }
}
