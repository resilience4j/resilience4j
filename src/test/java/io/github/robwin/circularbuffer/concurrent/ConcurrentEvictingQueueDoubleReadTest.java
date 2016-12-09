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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.testing.threadtester.AnnotatedTestRunner;
import com.google.testing.threadtester.ThreadedAfter;
import com.google.testing.threadtester.ThreadedBefore;
import com.google.testing.threadtester.ThreadedMain;
import com.google.testing.threadtester.ThreadedSecondary;
import io.github.robwin.circularbuffer.ConcurrentEvictingQueue;
import org.junit.Test;

public class ConcurrentEvictingQueueDoubleReadTest {

    ConcurrentEvictingQueue<Integer> queue;
    private Integer first;
    private Integer second;

    @Test
    public void concurrentEvictingQueueDoubleWriteTest() {
        AnnotatedTestRunner runner = new AnnotatedTestRunner();
        runner.runTests(getClass(), ConcurrentEvictingQueue.class);
    }

    @ThreadedBefore
    public void setup() {
        queue = new ConcurrentEvictingQueue<>(2);
        queue.offer(1);
    }

    @ThreadedMain
    public void firstActor() {
        first = queue.poll();
    }

    @ThreadedSecondary
    public void secondActor() {
        second = queue.poll();
    }

    @ThreadedAfter
    public void arbiter() {
        assertThat(asList(first, second)).containsOnly(1, null);
    }
}
