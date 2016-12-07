package io.github.robwin.circuitbreaker.internal;

import static java.lang.reflect.Array.newInstance;
import static java.util.Objects.requireNonNull;

import java.util.AbstractQueue;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

/**
 *
 * The purpose of this queue is to store the N most recently inserted elements.
 * If the {@link ConcurrentCircularBuffer} is already full {@code ConcurrentCircularBuffer.size() == capacity},
 * the oldest element (the head) will be evicted, and then the new element added at the tail.
 *
 * In order to achieve thread-safety it utilizes capability-based locking features of {@link StampedLock}.
 * All spins optimistic/pessimistic reads and writes are encapsulated in flowing methods:
 *
 * <ul>
 * <li> {@link ConcurrentCircularBuffer#readConcurrently(Supplier)}</li>
 * <li> {@link ConcurrentCircularBuffer#readConcurrentlyWithoutSpin(Supplier)}</li>
 * <li> {@link ConcurrentCircularBuffer#writeConcurrently(Supplier)}</li>
 * </ul>
 *
 * All other logic just relies on this utility methods.
 *
 * Also please take into account that {@link ConcurrentCircularBuffer#size}
 * and {@link ConcurrentCircularBuffer#modificationsCount} are {@code volatile} fields,
 * so we can read them and compare against them without any additional synchronizations.
 *
 * This class IS thread-safe, and does NOT accept null elements.
 *
 * @author bstorozhuk
 */
@SuppressWarnings("Duplicates")
public class ConcurrentCircularBuffer<E> extends AbstractQueue<E> {

    private static final String ILLEGAL_CAPACITY = "Capacity must be bigger than 0";
    private static final String ILLEGAL_ELEMENT = "Element must not be null";
    private static final String ILLEGAL_DESTINATION_ARRAY = "Destination array must not be null";
    private static final int RETRIES = 5;

    private final int maxSize;
    private volatile int size;
    private volatile int modificationsCount;
    private final StampedLock stampedLock;
    private Object[] ringBuffer;
    private int headIndex;
    private int tailIndex;

    public ConcurrentCircularBuffer(int capacity) {
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

    @Override
    public Iterator<E> iterator() {
        return readConcurrently(() -> new Iter(headIndex, modificationsCount));
    }

    @Override
    public int size() {
        return size;
    }

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

    @Override
    public void clear() {
        Supplier<Object> clearStrategy = () -> {
            if (size == 0) {
                return null;
            }
            ringBuffer = new Object[maxSize];
            size = 0;
            headIndex = 0;
            tailIndex = 0;
            modificationsCount++;
            return null;
        };
        writeConcurrently(clearStrategy);
    }

    @Override
    public Object[] toArray() {
        if (size == 0) {
            return new Object[0];
        }
        Object[] copy = new Object[size];
        Object[] destination = toArray(copy);
        return destination;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T[] toArray(final T[] destination) {
        requireNonNull(destination, ILLEGAL_DESTINATION_ARRAY);

        Supplier<T[]> copyRingBuffer = () -> {
            T[] result = destination;
            if (size == 0) {
                return result;
            }
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
        return (ringIndex + 1) % maxSize;
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

    private <T> T readConcurrently(final Supplier<T> readSupplier) {
        T result;
        long stamp;
        for (int i = 0; i < RETRIES; i++) {
            stamp = stampedLock.tryOptimisticRead();
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
}
