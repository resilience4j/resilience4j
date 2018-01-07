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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RingBitSetTest {

    @Test
    public void testRingBitSet() {
        RingBitSet ringBitSet = new RingBitSet(4);
        // The initial index is -1
        assertThat(ringBitSet.getIndex()).isEqualTo(-1);
        assertThat(ringBitSet.setNextBit(true)).isEqualTo(1);

        assertThat(ringBitSet.getIndex()).isEqualTo(0);
        assertThat(ringBitSet.setNextBit(false)).isEqualTo(1);

        assertThat(ringBitSet.getIndex()).isEqualTo(1);
        assertThat(ringBitSet.setNextBit(true)).isEqualTo(2);

        assertThat(ringBitSet.getIndex()).isEqualTo(2);
        assertThat(ringBitSet.setNextBit(true)).isEqualTo(3);

        assertThat(ringBitSet.getIndex()).isEqualTo(3);

        assertThat(ringBitSet.setNextBit(false)).isEqualTo(2);
        // The index has reached the maximum size and is set back to 0
        assertThat(ringBitSet.getIndex()).isEqualTo(0);
        assertThat(ringBitSet.setNextBit(false)).isEqualTo(2);
        assertThat(ringBitSet.getIndex()).isEqualTo(1);

        // The cardinality must be 2 because the first true was overwritten by the 5th setNextBit()
        assertThat(ringBitSet.cardinality()).isEqualTo(2);

        // The size is 64-bit, because the bits are stored in an array of one long value
        assertThat(ringBitSet.size()).isEqualTo(64);

        // The length must be 4, because the ring bit set contains 4 entries
        assertThat(ringBitSet.length()).isEqualTo(4);
    }

    @Test
    public void testRingBitSetParallel() {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
        RingBitSet ringBitSet = new RingBitSet(1000);
        IntStream.range(0, 1000).parallel().forEach((i) -> {
            if (i < 500) {
                ringBitSet.setNextBit(true);
            } else {
                ringBitSet.setNextBit(false);
            }
        });
        // The cardinality must be 500
        assertThat(ringBitSet.cardinality()).isEqualTo(500);

        // The size is 1024-bit, because the bits are stored in an array of 16 long values
        assertThat(ringBitSet.size()).isEqualTo(1024);

        // The length must be 1000, because the ring bit set contains 1000 entries
        assertThat(ringBitSet.length()).isEqualTo(1000);
    }

    @Test
    public void testRingBitSetWithSlightlyLessCapacity() {
        RingBitSet ringBitSet = new RingBitSet(100);
        long expectedCardinality = Stream.generate(ThreadLocalRandom.current()::nextBoolean)
            .limit(1000)
            .peek(ringBitSet::setNextBit)
            .skip(900)
            .mapToInt(b -> b ? 1 : 0)
            .sum();

        assertThat(ringBitSet.cardinality()).isEqualTo((int) expectedCardinality);
        assertThat(ringBitSet.size()).isEqualTo(128);
        assertThat(ringBitSet.length()).isEqualTo(100);
    }

    @Test
    public void testRingBitSetCopyFromTheSameSize() {
        RingBitSet sourceSet = new RingBitSet(4);
        // The initial index is -1
        assertThat(sourceSet.getIndex()).isEqualTo(-1);
        assertThat(sourceSet.toString()).isEqualTo("0000");

        assertThat(sourceSet.setNextBit(true)).isEqualTo(1);
        assertThat(sourceSet.toString()).isEqualTo("1000");

        assertThat(sourceSet.getIndex()).isEqualTo(0);
        assertThat(sourceSet.setNextBit(false)).isEqualTo(1);
        assertThat(sourceSet.toString()).isEqualTo("1000");

        assertThat(sourceSet.getIndex()).isEqualTo(1);
        assertThat(sourceSet.setNextBit(true)).isEqualTo(2);
        assertThat(sourceSet.toString()).isEqualTo("1010");

        assertThat(sourceSet.getIndex()).isEqualTo(2);
        assertThat(sourceSet.setNextBit(true)).isEqualTo(3);
        assertThat(sourceSet.toString()).isEqualTo("1011");

        assertThat(sourceSet.getIndex()).isEqualTo(3);
        assertThat(sourceSet.setNextBit(false)).isEqualTo(2);
        assertThat(sourceSet.toString()).isEqualTo("0011");

        // The index has reached the maximum size and is set back to 0
        assertThat(sourceSet.getIndex()).isEqualTo(0);
        assertThat(sourceSet.setNextBit(false)).isEqualTo(2);
        assertThat(sourceSet.toString()).isEqualTo("0011");

        assertThat(sourceSet.getIndex()).isEqualTo(1);

        RingBitSet setCopy = new RingBitSet(4, sourceSet);

        assertThat(setCopy.getIndex()).isEqualTo(3);
        assertThat(setCopy.toString()).isEqualTo("0011");

        assertThat(setCopy.cardinality()).isEqualTo(2);

        // The size is 64-bit, because the bits are stored in an array of one long value
        assertThat(setCopy.size()).isEqualTo(64);

        // The length must be 4, because the ring bit set contains 4 entries
        assertThat(setCopy.length()).isEqualTo(4);
    }

    @Test
    public void testRingBitSetCopyFromTheLongerSet() {
        RingBitSet sourceSet = new RingBitSet(5);
        // The initial index is -1
        assertThat(sourceSet.getIndex()).isEqualTo(-1);
        assertThat(sourceSet.toString()).isEqualTo("00000");

        assertThat(sourceSet.setNextBit(true)).isEqualTo(1);
        assertThat(sourceSet.toString()).isEqualTo("10000");

        assertThat(sourceSet.getIndex()).isEqualTo(0);
        assertThat(sourceSet.setNextBit(false)).isEqualTo(1);
        assertThat(sourceSet.toString()).isEqualTo("10000");

        assertThat(sourceSet.getIndex()).isEqualTo(1);
        assertThat(sourceSet.setNextBit(true)).isEqualTo(2);
        assertThat(sourceSet.toString()).isEqualTo("10100");

        assertThat(sourceSet.getIndex()).isEqualTo(2);
        assertThat(sourceSet.setNextBit(true)).isEqualTo(3);
        assertThat(sourceSet.toString()).isEqualTo("10110");

        assertThat(sourceSet.getIndex()).isEqualTo(3);
        assertThat(sourceSet.setNextBit(false)).isEqualTo(3);
        assertThat(sourceSet.toString()).isEqualTo("10110");

        assertThat(sourceSet.getIndex()).isEqualTo(4);
        assertThat(sourceSet.setNextBit(false)).isEqualTo(2);
        assertThat(sourceSet.toString()).isEqualTo("00110");

        // The index has reached the maximum size and is set back to 0
        assertThat(sourceSet.getIndex()).isEqualTo(0);
        assertThat(sourceSet.setNextBit(true)).isEqualTo(3);
        assertThat(sourceSet.toString()).isEqualTo("01110");

        assertThat(sourceSet.getIndex()).isEqualTo(1);

        RingBitSet setCopy = new RingBitSet(4, sourceSet);

        assertThat(setCopy.getIndex()).isEqualTo(3);
        assertThat(setCopy.toString()).isEqualTo("1001");

        assertThat(setCopy.cardinality()).isEqualTo(2);

        // The size is 64-bit, because the bits are stored in an array of one long value
        assertThat(setCopy.size()).isEqualTo(64);

        // The length must be 4, because the ring bit set contains 4 entries
        assertThat(setCopy.length()).isEqualTo(4);
    }

    @Test
    public void testRingBitSetCopyFromTheShorterSet() {
        RingBitSet sourceSet = new RingBitSet(3);
        // The initial index is -1
        assertThat(sourceSet.getIndex()).isEqualTo(-1);
        assertThat(sourceSet.toString()).isEqualTo("000");

        assertThat(sourceSet.setNextBit(true)).isEqualTo(1);
        assertThat(sourceSet.toString()).isEqualTo("100");

        assertThat(sourceSet.getIndex()).isEqualTo(0);
        assertThat(sourceSet.setNextBit(false)).isEqualTo(1);
        assertThat(sourceSet.toString()).isEqualTo("100");

        assertThat(sourceSet.getIndex()).isEqualTo(1);
        assertThat(sourceSet.setNextBit(true)).isEqualTo(2);
        assertThat(sourceSet.toString()).isEqualTo("101");

        assertThat(sourceSet.getIndex()).isEqualTo(2);
        assertThat(sourceSet.setNextBit(true)).isEqualTo(2);
        assertThat(sourceSet.toString()).isEqualTo("101");

        assertThat(sourceSet.getIndex()).isEqualTo(0);
        assertThat(sourceSet.setNextBit(false)).isEqualTo(2);
        assertThat(sourceSet.toString()).isEqualTo("101");

        assertThat(sourceSet.getIndex()).isEqualTo(1);

        RingBitSet setCopy = new RingBitSet(4, sourceSet);

        assertThat(setCopy.getIndex()).isEqualTo(2);
        assertThat(setCopy.toString()).isEqualTo("0110");

        assertThat(setCopy.cardinality()).isEqualTo(2);

        // The size is 64-bit, because the bits are stored in an array of one long value
        assertThat(setCopy.size()).isEqualTo(64);

        // The length must be 3, because the ring bit set contains only 3 entries after copying
        assertThat(setCopy.length()).isEqualTo(3);
    }
}
