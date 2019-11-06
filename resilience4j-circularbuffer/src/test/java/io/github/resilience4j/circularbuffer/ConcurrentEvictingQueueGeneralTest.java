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
package io.github.resilience4j.circularbuffer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;


public class ConcurrentEvictingQueueGeneralTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test(expected = IllegalArgumentException.class)
    public void zeroCapacity() throws Exception {
        new ConcurrentEvictingQueue<Integer>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCapacity() throws Exception {
        new ConcurrentEvictingQueue<Integer>(-1);
    }


    @Test
    public void iterator() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(2);
        queue.addAll(asList(1, 2, 3, 4, 5));
        Iterator<Integer> iterator = queue.iterator();
        assertThat(iterator.hasNext()).isTrue();
        Integer first = iterator.next();
        assertThat(first).isEqualTo(4);

        exception.expect(UnsupportedOperationException.class);
        iterator.remove();
    }

    @Test
    public void iteratorIllegalNext() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(2);
        queue.addAll(asList(1, 2, 3, 4, 5));
        Iterator<Integer> iterator = queue.iterator();
        assertThat(iterator.hasNext()).isTrue();
        Integer first = iterator.next();
        assertThat(first).isEqualTo(4);

        assertThat(iterator.hasNext()).isTrue();
        Integer second = iterator.next();
        assertThat(second).isEqualTo(5);

        assertThat(iterator.hasNext()).isFalse();
        exception.expect(NoSuchElementException.class);
        iterator.next();
    }

    @Test
    public void iteratorModificationAdd() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(2);
        queue.addAll(asList(4, 5));
        Iterator<Integer> iterator = queue.iterator();

        queue.peek();

        assertThat(iterator.hasNext()).isTrue();
        Integer first = iterator.next();
        assertThat(first).isEqualTo(4);

        queue.add(6);
        assertThat(iterator.hasNext()).isTrue();

        exception.expect(ConcurrentModificationException.class);
        iterator.next();
    }

    @Test
    public void iteratorModificationPoll() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(2);
        queue.addAll(asList(4, 5));
        Iterator<Integer> iterator = queue.iterator();

        assertThat(iterator.hasNext()).isTrue();
        Integer element = iterator.next();
        assertThat(element).isEqualTo(4);

        queue.poll();

        exception.expect(ConcurrentModificationException.class);
        iterator.next();
    }

    @Test
    public void poll() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(2);
        assertThat(queue.size()).isEqualTo(0);

        queue.add(1);
        assertThat(queue.size()).isEqualTo(1);

        queue.add(2);
        assertThat(queue.size()).isEqualTo(2);

        queue.addAll(asList(3, 4, 5));
        assertThat(queue.size()).isEqualTo(2);

        Integer peek = queue.peek();
        assertThat(peek).isEqualTo(4);
        assertThat(queue.size()).isEqualTo(2);

        Integer poll = queue.poll();
        assertThat(poll).isEqualTo(4);
        assertThat(queue.size()).isEqualTo(1);

        Integer secondPoll = queue.poll();
        assertThat(secondPoll).isEqualTo(5);
        assertThat(queue.size()).isEqualTo(0);

        Integer emptyPoll = queue.poll();
        assertThat(emptyPoll).isNull();
        assertThat(queue.size()).isEqualTo(0);

        queue.add(1);
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    public void offer() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(3);
        assertThat(queue.isEmpty()).isTrue();

        boolean offer = queue.offer(1);
        assertThat(offer).isTrue();
        assertThat(queue.toArray()).containsExactly(1);
        assertThat(queue.size()).isEqualTo(1);

        queue.offer(2);
        assertThat(queue.toArray()).containsExactly(1, 2);
        assertThat(queue.size()).isEqualTo(2);

        queue.offer(3);
        assertThat(queue.toArray()).containsExactly(1, 2, 3);
        assertThat(queue.size()).isEqualTo(3);

        queue.offer(4);
        assertThat(queue.toArray()).containsExactly(2, 3, 4);
        assertThat(queue.size()).isEqualTo(3);
    }

    @Test
    public void offerWithOneLength() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(1);
        assertThat(queue.size()).isEqualTo(0);

        queue.offer(1);
        assertThat(queue.toArray()).containsExactly(1);
        assertThat(queue.size()).isEqualTo(1);

        queue.offer(2);
        assertThat(queue.toArray()).containsExactly(2);
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    public void peek() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(2);

        Integer emptyPeek = queue.peek();
        assertThat(emptyPeek).isNull();

        queue.offer(1);
        assertThat(queue.toArray()).containsExactly(1);

        Integer first = queue.peek();
        assertThat(first).isEqualTo(1);
        assertThat(queue.toArray()).containsExactly(1);

        queue.offer(2);
        assertThat(queue.toArray()).containsExactly(1, 2);

        Integer second = queue.peek();
        assertThat(second).isEqualTo(1);
        assertThat(queue.toArray()).containsExactly(1, 2);
    }

    @Test
    public void clear() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(2);
        assertThat(queue).isEmpty();
        queue.clear();
        assertThat(queue).isEmpty();

        queue.offer(1);
        assertThat(queue.toArray()).containsExactly(1);

        queue.offer(2);
        assertThat(queue.toArray()).containsExactly(1, 2);

        queue.clear();
        assertThat(queue).isEmpty();
        assertThat(queue.size()).isEqualTo(0);

        queue.offer(3);
        assertThat(queue.toArray()).containsExactly(3);
    }

    @Test
    public void toArray() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(5);
        Object[] objects = queue.toArray();
        assertThat(objects.length).isEqualTo(0);

        queue.add(1);
        assertThat(queue.toArray()).containsExactly(1);
        queue.clear();

        queue.addAll(asList(1, 2, 3, 4, 5));
        assertThat(queue.toArray()).containsExactly(1, 2, 3, 4, 5);
        queue.clear();
        assertThat(queue).isEmpty();

        queue.addAll(asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertThat(queue.toArray()).containsExactly(5, 6, 7, 8, 9);
        queue.clear();
        assertThat(queue).isEmpty();
    }

    @Test
    public void toPreAllocatedArray() throws Exception {
        Queue<Integer> queue = new ConcurrentEvictingQueue<>(5);

        Integer[] emptyArray = queue.toArray(new Integer[]{});
        assertThat(emptyArray.length).isEqualTo(0);

        queue.add(1);
        assertThat(queue.toArray()).containsExactly(1);
        queue.clear();

        queue.addAll(asList(1, 2, 3, 4, 5));

        Integer[] first = new Integer[5];
        queue.toArray(first);
        assertThat(first).containsExactly(1, 2, 3, 4, 5);

        Integer[] second = new Integer[7];
        queue.toArray(second);
        assertThat(second).containsExactly(1, 2, 3, 4, 5, null, null);

        Integer[] third = new Integer[2];
        Integer[] thirdResult = queue.toArray(third);
        assertThat(third).containsExactly(null, null);
        assertThat(thirdResult).containsExactly(1, 2, 3, 4, 5);

        queue.clear();
        assertThat(queue).isEmpty();

        queue.addAll(asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Integer[] fourth = {11, 22, 33, 44, 55, 66, 77, 88};
        assertThat(queue.toArray(fourth)).containsExactly(5, 6, 7, 8, 9, 66, 77, 88);
        queue.clear();
        assertThat(queue).isEmpty();
    }
}