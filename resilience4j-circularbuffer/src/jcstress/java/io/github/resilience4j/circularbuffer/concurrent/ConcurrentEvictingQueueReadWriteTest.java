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

import java.util.Arrays;

@JCStressTest
@State
@Outcome(id = "[1, 2], [2]", expect = Expect.ACCEPTABLE)
@Outcome(id = "[2], [2]", expect = Expect.ACCEPTABLE)
public class ConcurrentEvictingQueueReadWriteTest {

    ConcurrentEvictingQueue<Integer> queue;
    private Object[] array;

    public ConcurrentEvictingQueueReadWriteTest() {
        queue = new ConcurrentEvictingQueue<>(2);
        queue.offer(1);
        queue.offer(2);
    }

    @Actor
    public void firstActor() {
        queue.poll();
    }

    @Actor
    public void secondActor() {
        array = queue.toArray();
    }

    @Arbiter
    public void arbiter(StringResult2 result) {
        result.r1 = Arrays.toString(array);
        result.r2 = queue.toString();
    }
}
