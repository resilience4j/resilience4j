/*
 *
 *  Copyright 2017 Robert Winkler, Bohdan Storozhuk, Lucas Lech
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
package io.github.resilience4j.bulkhead.concurrent;

import io.github.resilience4j.adapter.RxJava2Adapter;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadEvent.Type;
import io.reactivex.subscribers.TestSubscriber;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.StringResult1;

import java.text.MessageFormat;


@JCStressTest
@State
@Outcome.Outcomes(
    {
        @Outcome(
            id = "remainingDepth=1" +
                " events=\\[\\[CALL_REJECTED\\], \\[\\], \\[\\]\\]",
            expect = Expect.ACCEPTABLE
        ),
        @Outcome(
            id = "remainingDepth=1" +
                " events=\\[\\[\\], \\[\\], \\[\\]\\]",
            expect = Expect.ACCEPTABLE
        )
    }
)
public class ConcurrentBulkheadTest {

    private Bulkhead bulkhead;
    private TestSubscriber<Type> callRejectectedEventSubscriber;

    public ConcurrentBulkheadTest() {

        bulkhead = Bulkhead.of("test", BulkheadConfig.custom().maxConcurrentCalls(1).build());

        callRejectectedEventSubscriber = RxJava2Adapter.toFlowable(bulkhead.getEventPublisher())
            .filter(event -> event.getEventType() == Type.CALL_REJECTED)
            .map(BulkheadEvent::getEventType)
            .test();
    }

    @Actor
    public void firstActor() {
        if (bulkhead.acquirePermission()) {
            bulkhead.onComplete();
        }
    }

    @Actor
    public void secondActor() {
        if (bulkhead.acquirePermission()) {
            bulkhead.onComplete();
        }
    }

    @Arbiter
    public void arbiter(StringResult1 result1) {
        String result = MessageFormat.format(
            "remainingDepth={0} events={1}",
            bulkhead.getMetrics().getAvailableConcurrentCalls(),
            callRejectectedEventSubscriber.getEvents()
        );
        result1.r1 = result;
    }
}
