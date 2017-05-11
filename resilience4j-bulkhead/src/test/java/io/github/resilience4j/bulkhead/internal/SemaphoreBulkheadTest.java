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
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.*;

import static org.assertj.core.api.Assertions.assertThat;

public class SemaphoreBulkheadTest {

    private Bulkhead bulkhead;
    private TestSubscriber<BulkheadEvent.Type> testSubscriber;

    @Before
    public void setUp(){

        BulkheadConfig config = BulkheadConfig.custom()
                                              .maxConcurrentCalls(2)
                                              .maxWaitTime(0)
                                              .build();

        bulkhead = Bulkhead.of("test", config);
        testSubscriber = bulkhead.getEventStream()
                                 .map(BulkheadEvent::getEventType)
                                 .test();
    }

    @Test
    public void shouldReturnTheCorrectName() {
        assertThat(bulkhead.getName()).isEqualTo("test");
    }

    @Test
    public void testBulkhead() throws InterruptedException {

        bulkhead.isCallPermitted();
        bulkhead.isCallPermitted();

        assertThat(bulkhead.getAvailableConcurrentCalls()).isEqualTo(0);

        bulkhead.isCallPermitted();
        bulkhead.onComplete();

        assertThat(bulkhead.getAvailableConcurrentCalls()).isEqualTo(1);

        bulkhead.onComplete();

        assertThat(bulkhead.getAvailableConcurrentCalls()).isEqualTo(2);

        bulkhead.isCallPermitted();

        testSubscriber.assertValueCount(4)
                      .assertValues(CALL_PERMITTED, CALL_PERMITTED, CALL_REJECTED, CALL_PERMITTED);
    }

    @Test
    public void testToString() {

        // when
        String result = bulkhead.toString();

        // then
        assertThat(result).isEqualTo("Bulkhead 'test'");
    }

    @Test
    public void testCreateWithNullConfig() {

        // given
        Supplier<BulkheadConfig> configSupplier = () -> null;

        // when
        Bulkhead bulkhead = Bulkhead.of("test", configSupplier);

        // then
        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getBulkheadConfig()).isNotNull();
    }

    @Test
    public void testCreateWithDefaults() {

        // when
        Bulkhead bulkhead = Bulkhead.ofDefaults("test");

        // then
        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getBulkheadConfig()).isNotNull();
    }

    @Test
    public void testTryEnterWithTimeout() {

        // given
        BulkheadConfig config = BulkheadConfig.custom()
                                              .maxConcurrentCalls(1)
                                              .maxWaitTime(100)
                                              .build();

        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", config);

        // when
        boolean entered = bulkhead.tryEnterBulkhead();

        // then
        assertThat(entered).isTrue();
    }

    @Test
    public void testEntryTimeout() {

        // given
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitTime(10)
                .build();

        SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", config);
        bulkhead.isCallPermitted(); // consume the permit

        // when
        boolean entered = bulkhead.tryEnterBulkhead();

        // then
        assertThat(entered).isFalse();
    }

    @Test // best effort, no asserts
    public void testEntryInterrupted() {

        // given
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitTime(10000)
                .build();

        final SemaphoreBulkhead bulkhead = new SemaphoreBulkhead("test", config);
        bulkhead.isCallPermitted(); // consume the permit
        AtomicBoolean entered = new AtomicBoolean(true);

        Thread t = new Thread(
                       () -> {
                           entered.set(bulkhead.tryEnterBulkhead());
                       }
                   );

        // when
        t.start();
        sleep(500);
        t.interrupt();
        sleep(500);

        // then
        //assertThat(entered.get()).isFalse();
    }

    void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
