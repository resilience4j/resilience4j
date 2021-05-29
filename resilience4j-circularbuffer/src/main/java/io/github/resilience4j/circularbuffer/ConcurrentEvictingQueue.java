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

import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import static java.lang.reflect.Array.newInstance;
import static java.util.Objects.requireNonNull;

/**
 * The purpose of this queue is to store the N most recently inserted elements. If the {@link
 * ConcurrentEvictingQueue} is already full {@code ConcurrentEvictingQueue.size() == capacity}, the
 * oldest element (the head) will be evicted, and then the new element added at the tail.
 * <p>
 * In order to achieve thread-safety it utilizes capability-based locking features of {@link
 * StampedLock}. All spins optimistic/pessimistic reads and writes are encapsulated in following
 * methods:
 *
 * <ul>
 * <li> {@link ConcurrentEvictingQueue#readConcurrently(Supplier)}</li>
 * <li> {@link ConcurrentEvictingQueue#readConcurrentlyWithoutSpin(Supplier)}</li>
 * <li> {@link ConcurrentEvictingQueue#writeConcurrently(Supplier)}</li>
 * </ul>
 * <p>
 * All other logic just relies on this utility methods.
 * <p>
 * Also please take into account that {@link ConcurrentEvictingQueue#size}
 * and {@link ConcurrentEvictingQueue#modificationsCount} are {@code volatile} fields,
 * so we can read them and compare against them without any additional synchronizations.
 * <p>
 * This class IS thread-safe, and does NOT accept null elements.
 */
public class ConcurrentEvictingQueue<E> extends AbstractQueue<E> {

    private static final String ILLEGAL_CAPACITY = "Capacity must be bigger than 0";
    private static final String ILLEGAL_ELEMENT = "Element must not be null";
    private static final String ILLEGAL_DESTINATION_ARRAY = "Destination array must not be null";
    private static final Object[] DEFAULT_DESTINATION = new Object[0];
    private static final int RETRIES = 5;

    private final int maxSize;
    private final StampedLock stampedLock;
    private volatile int size;
    private Object[] ringBuffer;
    private int headIndex;
    private int tailIndex;
    private int modificationsCount;

    public ConcurrentEvictingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(ILLEGAL_CAPACITY);
        }
        maxSize = capacity;
        ringBuffer = new Object[capacity];
        size = 0;
        headIndex = 0;
        tailIndex = 0;
        modificationsCount = 0;
        stampedLock = new StampedLock();
    }

    /**
     * Returns an iterator over the elements in this queue in proper sequence. The elements will be
     * returned in order from first (head) to last (tail).
     * <p>
     * This iterator implementation NOT allow removes and co-modifications.
     *
     * @return an iterator over the elements in this queue in proper sequence
     */
    @Override
    public Iterator<E> iterator() {
        return readConcurrently(() -> new Iter(headIndex, modificationsCount));
    }

    /**
     * Returns the number of elements in this queue.
     *
     * @return the number of elements in this queue
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Inserts the specified element at the tail of this queue if it is possible to do so
     * immediately or if capacity limit is exited the oldest element (the head) will be evicted, and
     * then the new element added at the tail. This method is generally preferable to method {@link
     * #add}, which can fail to insert an element only by throwing an exception.
     *
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean offer(final E e) {
        requireNonNull(e, ILLEGAL_ELEMENT);

        Supplier<Boolean> offerElement = () -> {
            if (size == 0) {
                ringBuffer[tailIndex] = e;
                modificationsCount++;
                size++;
            } else if (size == maxSize) {
                headIndex = nextIndex(headIndex);
                tailIndex = nextIndex(tailIndex);
                ringBuffer[tailIndex] = e;
                modificationsCount++;
            } else {
                tailIndex = nextIndex(tailIndex);
                ringBuffer[tailIndex] = e;
                size++;
                modificationsCount++;
            }
            return true;
        };
        return writeConcurrently(offerElement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public E poll() {
        Supplier<E> pollElement = () -> {
            if (size == 0) {
                return null;
            }
            E result = (E) ringBuffer[headIndex];
            ringBuffer[headIndex] = null;
            if (size != 1) {
                headIndex = nextIndex(headIndex);
            }
            size--;
            modificationsCount++;
            return result;
        };
        return writeConcurrently(pollElement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public E peek() {
        return readConcurrently(() -> {
            if (size == 0) {
                return null;
            }
            return (E) this.ringBuffer[this.headIndex];
        });
    }

    /**
     * Atomically removes all of the elements from this queue. The queue will be empty after this
     * call returns.
     */
    @Override
    public void clear() {
        Supplier<Object> clearStrategy = () -> {
            if (size == 0) {
                return null;
            }
            Arrays.fill(ringBuffer, null);
            size = 0;
            headIndex = 0;
            tailIndex = 0;
            modificationsCount++;
            return null;
        };
        writeConcurrently(clearStrategy);
    }

    /**
     * Returns an array containing all of the elements in this queue, in proper sequence.
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate a new array).  The
     * caller is free to modify the returned array.
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    @Override
    public Object[] toArray() {
        if (size == 0) {
            return new Object[0];
        }
        Object[] destination = toArray(DEFAULT_DESTINATION);
        return destination;
    }

    /**
     * Returns an array containing all of the elements in this queue, in proper sequence; the
     * runtime type of the returned array is that of the specified array.  If the queue fits in the
     * specified array, it is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this queue.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows precise control over the
     * runtime type of the output array, and may, under certain circumstances, be used to save
     * allocation costs.
     * <p>
     * Note that {@code toArray(new Object[0])} is identical in function to {@code toArray()}.
     *
     * @param destination the array into which the elements of the queue are to be stored, if it is
     *                    big enough; otherwise, a new array of the same runtime type is allocated
     *                    for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException  if the runtime type of the specified array is not a supertype of
     *                              the runtime type of every element in this queue
     * @throws NullPointerException if the specified array is null
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T[] toArray(final T[] destination) {
        requireNonNull(destination, ILLEGAL_DESTINATION_ARRAY);

        Supplier<T[]> copyRingBuffer = () -> {
            if (size == 0) {
                return destination;
            }
            T[] result = destination;
            if (destination.length < size) {
                result = (T[]) newInstance(result.getClass().getComponentType(), size);
            }
            if (headIndex <= tailIndex) {
                System.arraycopy(ringBuffer, headIndex, result, 0, size);
            } else {
                int toTheEnd = ringBuffer.length - headIndex;
                System.arraycopy(ringBuffer, headIndex, result, 0, toTheEnd);
                System.arraycopy(ringBuffer, 0, result, toTheEnd, tailIndex + 1);
            }
            return result;
        };
        return readConcurrentlyWithoutSpin(copyRingBuffer);
    }

    private int nextIndex(final int ringIndex) {
        int nextIdx = ringIndex + 1;
        if (nextIdx == maxSize) {
            return 0;
        }
        return nextIdx;
    }

    private <T> T readConcurrently(final Supplier<T> readSupplier) {
        T result;
        long stamp;
        for (int i = 0; i < RETRIES; i++) {
            stamp = stampedLock.tryOptimisticRead();
            if (stamp == 0) {
                continue;
            }
            result = readSupplier.get();
            if (stampedLock.validate(stamp)) {
                return result;
            }
        }
        stamp = stampedLock.readLock();
        try {
            result = readSupplier.get();
        } finally {
            stampedLock.unlockRead(stamp);
        }
        return result;
    }

    private <T> T readConcurrentlyWithoutSpin(final Supplier<T> readSupplier) {
        T result;
        long stamp = stampedLock.readLock();
        try {
            result = readSupplier.get();
        } finally {
            stampedLock.unlockRead(stamp);
        }
        return result;
    }

    private <T> T writeConcurrently(final Supplier<T> writeSupplier) {
        T result;
        long stamp = stampedLock.writeLock();
        try {
            result = writeSupplier.get();
        } finally {
            stampedLock.unlockWrite(stamp);
        }
        return result;
    }

    private class Iter implements Iterator<E> {

        private int visitedCount = 0;
        private int cursor;
        private int expectedModificationsCount;

        Iter(final int headIndex, final int modificationsCount) {
            this.cursor = headIndex;
            this.expectedModificationsCount = modificationsCount;
        }

        @Override
        public boolean hasNext() {
            return visitedCount < size;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            Supplier<E> nextElement = () -> {
                checkForModification();
                if (visitedCount >= size) {
                    throw new NoSuchElementException();
                }
                E item = (E) ringBuffer[cursor];
                cursor = nextIndex(cursor);
                visitedCount++;
                return item;
            };
            return readConcurrently(nextElement);
        }

        private void checkForModification() {
            if (modificationsCount != expectedModificationsCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
