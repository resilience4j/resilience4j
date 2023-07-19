package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.event.BulkheadOnLimitChangedEvent;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrencyLimitTest {

    private static final Throwable ERROR = new Throwable("test");

    private AdaptiveBulkheadStateMachine bulkhead;
    private final List<BulkheadOnLimitChangedEvent> limitChanges = new LinkedList<>();

    public void givenBulkhead(AdaptiveBulkheadConfig config) {
        bulkhead = (AdaptiveBulkheadStateMachine) AdaptiveBulkhead.of("test", config);
        bulkhead.getEventPublisher().onLimitChanged(limitChanges::add);
    }

    private void onSuccess() {
        bulkhead.onSuccess(bulkhead.getCurrentTimestamp(), bulkhead.getTimestampUnit());
    }

    private void onError() {
        bulkhead.onError(bulkhead.getCurrentTimestamp(), bulkhead.getTimestampUnit(), ERROR);
    }

    @Test
    public void testFastIncrease() {
        givenBulkhead(AdaptiveBulkheadConfig.custom()
            .minimumNumberOfCalls(1)
            .maxConcurrentCalls(100)
            .initialConcurrentCalls(1)
            .increaseMultiplier(1.9f)
            .build());

        for (int i = 0; i < 11; i++) {
            onSuccess();
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(2, 4, 8, 16, 31, 59, 100);
    }

    @Test
    public void testFastDecrease() {
        givenBulkhead(AdaptiveBulkheadConfig.custom()
            .minimumNumberOfCalls(1)
            .maxConcurrentCalls(100)
            .initialConcurrentCalls(100)
            .decreaseMultiplier(0.1f)
            .build());

        for (int i = 0; i < 11; i++) {
            onError();
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(10, 2);
    }

    @Test
    public void testSlowIncrease() {
        givenBulkhead(AdaptiveBulkheadConfig.custom()
            .minimumNumberOfCalls(1)
            .maxConcurrentCalls(100)
            .initialConcurrentCalls(1)
            .increaseMultiplier(1.1f)
            .build());

        for (int i = 0; i < 11; i++) {
            onSuccess();
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13);
    }

    @Test
    public void testSlowDecrease() {
        givenBulkhead(AdaptiveBulkheadConfig.custom()
            .minimumNumberOfCalls(1)
            .maxConcurrentCalls(100)
            .initialConcurrentCalls(100)
            .decreaseMultiplier(0.9f)
            .build());

        for (int i = 0; i < 11; i++) {
            onError();
        }

        assertThat(limitChanges)
            .extracting(BulkheadOnLimitChangedEvent::getNewMaxConcurrentCalls)
            .containsExactly(90, 81, 72, 64, 57, 51, 45, 40, 36, 32, 28);
    }

}