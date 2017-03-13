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
package io.github.robwin.circularbuffer.concurrent;

import com.google.testing.threadtester.*;
import io.github.robwin.circularbuffer.ConcurrentEvictingQueue;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentEvictingQueueDoubleWriteTest {

    ConcurrentEvictingQueue<Integer> queue;

    @Test
    public void concurrentEvictingQueueDoubleWriteTest() {
        AnnotatedTestRunner runner = new AnnotatedTestRunner();
        runner.runTests(getClass(), ConcurrentEvictingQueue.class);
    }

    @ThreadedBefore
    public void setUp() {
        queue = new ConcurrentEvictingQueue<>(3);
    }

    @ThreadedMain
    public void firstActor() {
         queue.offer(1);
    }

    @ThreadedSecondary
    public void secondActor() {
        queue.offer(2);
    }

    @ThreadedAfter
    public void arbiter() {
        Integer first = queue.poll();
        Integer second = queue.poll();
        assertThat(asList(first, second)).containsOnly(1, 2);
    }
}
