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
package io.github.resilience4j.bitset;

/**
 * A ring bit set which stores bits up to a maximum size of bits.
 */
public class RingBitSet {

    private final int size;
    private final BitSetMod bitSet;

    private boolean notFull;
    private int index = -1;

    private volatile int length;
    private volatile int cardinality = 0;


    /**
     * Creates a ring bit set whose size is large enough to explicitly
     * represent bits with indices in the range {@code 0} through
     * {@code bitSetSize-1}. All bits are initially {@code false}.
     *
     * @param bitSetSize the size of the ring bit set
     * @throws NegativeArraySizeException if the specified initial size
     *                                    is negative
     */
    public RingBitSet(int bitSetSize) {
        notFull = true;
        size = bitSetSize;
        bitSet = new BitSetMod(bitSetSize);
    }

    /**
     * Creates a ring bit set whose size is large enough to explicitly
     * represent bits with indices in the range {@code 0} through
     * {@code bitSetSize-1}. The bits from the source ring bit set are copied into the new ring bit set.
     *
     * @param bitSetSize the size of the ring bit set
     * @param sourceSet the source ring bit set
     * @throws NegativeArraySizeException if the specified initial size
     *                                    is negative
     */
    public RingBitSet(int bitSetSize, RingBitSet sourceSet) {
        this(bitSetSize);

        int targetLength = Integer.min(bitSetSize, sourceSet.length);
        int sourceIndex = sourceSet.index;
        int forwardIndex = sourceSet.size - sourceIndex;
        for (int i = 0; i < targetLength; i++) {
            this.setNextBit(sourceSet.bitSet.get(sourceIndex));
            // looping sourceIndex backwards without conditional statements
            forwardIndex = (forwardIndex + 1) % sourceSet.size;
            sourceIndex = (sourceSet.size - forwardIndex) % sourceSet.size;
        }
    }

    /**
     * Sets the bit at the next index to the specified value.
     *
     * @param value a boolean value to set
     * @return the number of bits set to {@code true}
     */
    public synchronized int setNextBit(boolean value) {
        increaseLength();
        index = (index + 1) % size;

        int previous = bitSet.set(index, value);
        int current = value ? 1 : 0;
        cardinality = cardinality - previous + current;
        return cardinality;
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code RingBitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code RingBitSet}
     */
    public int cardinality() {
        return cardinality;
    }

    /**
     * Returns the number of bits of space actually in use by this
     * {@code RingBitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this ring bit set
     */
    public int size() {
        return bitSet.size();
    }

    /**
     * Returns the "logical size" up to the maximum size of this {@code RingBitSet}.
     * Returns zero if the {@code RingBitSet} contains no set bits.
     *
     * @return the logical size of this {@code RingBitSet}
     */
    public int length() {
        return length;
    }

    /**
     * Prints the current state of internal bit set.
     * @return string representation on internal bit set.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < size; i++) {
            result.append(bitSet.get(i) ? '1' : '0');
        }
        return result.toString();
    }

    /**
     * Returns the current index of this {@code RingBitSet}.
     * Use only for debugging and testing
     *
     * @return the current index of this {@code RingBitSet}
     */
    synchronized int getIndex() {
        return index;
    }

    private void increaseLength() {
        if (notFull) {
            int nextLength = length + 1;
            if (nextLength < size) {
                length = nextLength;
            } else {
                length = size;
                notFull = false;
            }
        }
    }
}
