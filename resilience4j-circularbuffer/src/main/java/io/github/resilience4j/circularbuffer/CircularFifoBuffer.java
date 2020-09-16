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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A {@link CircularFifoBuffer} is a first in first out buffer with a fixed size that replaces its
 * oldest element if full. {@link CircularFifoBuffer} does NOT accept null elements.
 **/
public interface CircularFifoBuffer<T> {

    /**
     * Returns the number of elements in this {@link CircularFifoBuffer}.
     *
     * @return the number of elements in this {@link CircularFifoBuffer}
     */
    int size();

    /**
     * Returns <code>true</code> if this {@link CircularFifoBuffer} contains no elements.
     *
     * @return <code>true</code> if this {@link CircularFifoBuffer} contains no elements
     */
    boolean isEmpty();

    /**
     * Returns <code>true</code> if this {@link CircularFifoBuffer} is full.
     *
     * @return <code>true</code> if this {@link CircularFifoBuffer} is full
     */
    boolean isFull();

    /**
     * Returns a list containing all of the elements in this {@link CircularFifoBuffer}. The
     * elements are copied into an array.
     *
     * @return a list containing all of the elements in this {@link CircularFifoBuffer}
     */
    List<T> toList();

    /**
     * Returns a stream of the elements in this {@link CircularFifoBuffer}.
     *
     * @return a stream of the elements in this {@link CircularFifoBuffer}
     */
    Stream<T> toStream();

    /**
     * Adds element to the {@link CircularFifoBuffer} and overwrites the oldest element when {@link
     * CircularFifoBuffer#isFull}.
     *
     * @param element to add
     * @throws NullPointerException if the specified element is null
     */
    void add(T element);

    /**
     * Retrieves and removes the head of this queue, or returns {@link Optional#empty()} if this queue is
     * empty.
     *
     * @return the head of this queue, or {@link Optional#empty()} empty} if this queue is empty
     */
    Optional<T> take();
}
