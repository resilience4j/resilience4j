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

import java.util.BitSet;

/**
 * A ring bit set which stores bits up to a maximum size of bits.
 */
public class RingBitSet {

    private final BitSet bitSet;

    /**
     * The maximum size of the ring bit set
     */
    private final int maxSize;

    /**
     * The current index of the ring bit set
     */
    private int index = -1;


    /**
     * The current size of the ring bit set.
     */
    private int size = 0;


    /**
     * Creates a ring bit set whose size is large enough to explicitly
     * represent bits with indices in the range {@code 0} through
     * {@code bitSetSize-1}. All bits are initially {@code false}.
     *
     * @param  bitSetSize the size of the ring bit set
     * @throws NegativeArraySizeException if the specified initial size
     *         is negative
     */
    public RingBitSet(int bitSetSize) {
        this.maxSize = bitSetSize;
        this.bitSet = new BitSet(bitSetSize);
    }

    /**
     * Sets the bit at the next index to the specified value.
     *
     * @param  value a boolean value to set
     */
    public synchronized void setNextBit(boolean value) {
        this.bitSet.set(nextIndex(), value);
        this.increaseSize();
    }

    /**
     * Returns the current index of this {@code RingBitSet}.
     *
     * @return the current index of this {@code RingBitSet}
     */
    public synchronized int getIndex(){
        return index;
    }

    /**
     * Returns the next index. If the index reaches the maximum number of bits, it is set back to zero.
     * It is this behaviour which turns the bit set into a ring bit set.
     *
     * @return the next index
     */
    private synchronized int nextIndex(){
        index++;
        if (index == maxSize) index = 0;
        return index;
    }

    /**
     * Increases the size of this {@code RingBitSet} up to the maximum size of this {@code RingBitSet}.
     */
    private synchronized void increaseSize(){
        if(size < maxSize) size++;
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code RingBitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code RingBitSet}
     */
    public synchronized int cardinality(){
        return bitSet.cardinality();
    }

    /**
     * Returns the number of bits of space actually in use by this
     * {@code RingBitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this ring bit set
     */
    public synchronized int size(){
        return bitSet.size();
    }

    /**
     * Returns the "logical size" up to the maximum size of this {@code RingBitSet}.
     * Returns zero if the {@code RingBitSet} contains no set bits.
     *
     * @return the logical size of this {@code RingBitSet}
     */
    public synchronized int length(){
        return this.size;
    }
}
