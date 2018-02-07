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
 * {@link BitSetMod} is simplified version of {@link java.util.BitSet}.
 * It has no dynamic allocation, expanding logic, boundary checks
 * and it's set method returns previous bit state.
 */
class BitSetMod {

    private final static int ADDRESS_BITS_PER_WORD = 6;
    private final int size;
    private final long[] words;


    BitSetMod(final int capacity) {
        int countOfWordsRequired = wordIndex(capacity - 1) + 1;
        size = countOfWordsRequired << ADDRESS_BITS_PER_WORD;
        words = new long[countOfWordsRequired];
    }

    /**
     * Given a bit index, return word index containing it.
     */
    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    /**
     * Sets the bit at the specified index to value.
     *
     * @param bitIndex a bit index
     * @return previous state of bitIndex that can be {@code 1} or {@code 0}
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    int set(int bitIndex, boolean value) {
        int wordIndex = wordIndex(bitIndex);
        long bitMask = 1L << bitIndex;
        int previous = (words[wordIndex] & bitMask) != 0 ? 1 : 0;
        if (value) {
            words[wordIndex] |= bitMask;
        } else {
            words[wordIndex] &= ~bitMask;
        }
        return previous;
    }

    int size() {
        return size;
    }

    /**
     * Gets the bit at the specified index.
     *
     * @param bitIndex a bit index
     * @return state of bitIndex that can be {@code true} or {@code false}
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    boolean get(int bitIndex) {
        int wordIndex = wordIndex(bitIndex);
        long bitMask = 1L << bitIndex;
        return (words[wordIndex] & bitMask) != 0;
    }
}
