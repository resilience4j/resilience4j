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
import org.openjdk.jcstress.infra.results.StringResult1;

@JCStressTest
@State
@Outcome(id = "[1, 2]", expect = Expect.ACCEPTABLE)
@Outcome(id = "[2, 1]", expect = Expect.ACCEPTABLE)
public class ConcurrentEvictingQueueDoubleWriteTest {

    ConcurrentEvictingQueue<Integer> queue;

    public ConcurrentEvictingQueueDoubleWriteTest() {
        queue = new ConcurrentEvictingQueue<>(3);
    }

    @Actor
    public void firstActor() {
        queue.offer(1);
    }

    @Actor
    public void secondActor() {
        queue.offer(2);
    }

    @Arbiter
    public void arbiter(StringResult1 result) {
        result.r1 = queue.toString();
    }
}
