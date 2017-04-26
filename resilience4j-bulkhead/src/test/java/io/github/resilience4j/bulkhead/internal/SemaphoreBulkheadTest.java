/*
 *
 *  Copyright 2017 Lucas Lech
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
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Before;
import org.junit.Test;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.*;

import static org.assertj.core.api.Assertions.assertThat;

public class SemaphoreBulkheadTest {

    private Bulkhead bulkhead;
    private TestSubscriber<BulkheadEvent.Type> testSubscriber;

    @Before
    public void setUp(){
        bulkhead = Bulkhead.of("test", 2);
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

        assertThat(bulkhead.getRemainingDepth()).isEqualTo(0);

        bulkhead.isCallPermitted();
        bulkhead.onComplete();

        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);

        bulkhead.onComplete();

        assertThat(bulkhead.getRemainingDepth()).isEqualTo(2);

        bulkhead.isCallPermitted();

        testSubscriber
                .assertValueCount(4)
                .assertValues(CALL_PERMITTED, CALL_PERMITTED, CALL_REJECTED, CALL_PERMITTED);
    }

}
