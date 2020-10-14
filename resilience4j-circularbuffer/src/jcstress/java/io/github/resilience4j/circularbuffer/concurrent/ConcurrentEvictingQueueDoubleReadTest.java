/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.circularbuffer.concurrent;

import io.github.resilience4j.circularbuffer.ConcurrentEvictingQueue;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.StringResult2;

@JCStressTest
@State
@Outcome(id = "1, null", expect = Expect.ACCEPTABLE)
@Outcome(id = "null, 1", expect = Expect.ACCEPTABLE)
@Outcome(id = "null, null", expect = Expect.FORBIDDEN)
@Outcome(id = "1, 1", expect = Expect.FORBIDDEN)
public class ConcurrentEvictingQueueDoubleReadTest {

    ConcurrentEvictingQueue<Integer> queue;
    private Integer first;
    private Integer second;

    public ConcurrentEvictingQueueDoubleReadTest() {
        queue = new ConcurrentEvictingQueue<>(2);
        queue.offer(1);
    }

    @Actor
    public void firstActor() {
        first = queue.poll();
    }

    @Actor
    public void secondActor() {
        second = queue.poll();
    }

    @Arbiter
    public void arbiter(StringResult2 result2) {
        result2.r1 = first == null ? "null" : String.valueOf(first);
        result2.r2 = second == null ? "null" : String.valueOf(second);
    }
}
