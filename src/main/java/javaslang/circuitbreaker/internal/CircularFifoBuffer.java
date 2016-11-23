/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.circuitbreaker.internal;

import javaslang.collection.List;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A CircularFifoBuffer is a first in first out buffer with a fixed size that replaces its oldest element if full.
 **/
public class CircularFifoBuffer<T> {

    private final ArrayBlockingQueue<T> fifoQueue;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates an {@code CircularFifoBuffer} with the given (fixed)
     * capacity
     *
     * @param capacity the capacity of this CircularFifoBuffer
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public CircularFifoBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("CircularFifoBuffer capacity must be greater than 0");
        }
        fifoQueue = new ArrayBlockingQueue<>(capacity);
    }

    /**
     * Returns the number of elements in this CircularFifoBuffer.
     *
     * @return the number of elements in this CircularFifoBuffer
     */
    public int size() {
        return fifoQueue.size();
    }

    /**
     * Returns <tt>true</tt> if this CircularFifoBuffer contains no elements.
     *
     * @return <tt>true</tt> if this CircularFifoBuffer contains no elements
     */
    public boolean isEmpty() {
        return fifoQueue.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this CircularFifoBuffer is full.
     *
     * @return <tt>true</tt> if this CircularFifoBuffer is full
     */
    public boolean isFull() {
        return fifoQueue.remainingCapacity() == 0;
    }

    /**
     * Returns a list containing all of the elements in this CircularFifoBuffer.
     * The elements are copied into an array.
     *
     * @return a list containing all of the elements in this CircularFifoBuffer
     */
    public List<T> toList(){
        return List.ofAll(fifoQueue);
    }

    /**
     * Overwrites the oldest element when full.
     */
    public void add(T element) {
        if(!fifoQueue.offer(element)){
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                fifoQueue.remove();
                fifoQueue.add(element);
            } finally {
                lock.unlock();
            }
        }
    }
}