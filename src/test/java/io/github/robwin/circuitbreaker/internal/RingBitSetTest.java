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
package io.github.robwin.circuitbreaker.internal;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class RingBitSetTest {

    private static final Logger LOG = LoggerFactory.getLogger(RingBitSetTest.class);

    @Test
    public void testRingBitSet(){
        RingBitSet ringBitSet = new RingBitSet(4);
        // The initial index is -1
        assertThat(ringBitSet.getIndex()).isEqualTo(-1);
        ringBitSet.setNextBit(true);
        assertThat(ringBitSet.getIndex()).isEqualTo(0);
        ringBitSet.setNextBit(false);
        assertThat(ringBitSet.getIndex()).isEqualTo(1);
        ringBitSet.setNextBit(true);
        assertThat(ringBitSet.getIndex()).isEqualTo(2);
        ringBitSet.setNextBit(true);
        assertThat(ringBitSet.getIndex()).isEqualTo(3);


        ringBitSet.setNextBit(false);
        // The index has reached the maximum size and is set back to 0
        assertThat(ringBitSet.getIndex()).isEqualTo(0);
        ringBitSet.setNextBit(false);
        assertThat(ringBitSet.getIndex()).isEqualTo(1);

        // The cardinality must be 2 because the first true was overwritten by the 5th setNextBit()
        assertThat(ringBitSet.cardinality()).isEqualTo(2);

        // The size is 64-bit, because the bits are stored in an array of one long value
        assertThat(ringBitSet.size()).isEqualTo(64);

        // The length must be 4, because the ring bit set contains 4 entries
        assertThat(ringBitSet.length()).isEqualTo(4);
    }

    @Test
    public void testRingBitSetParallel(){
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
        RingBitSet ringBitSet = new RingBitSet(1000);
                IntStream.range(0, 1000).parallel().forEach((i) -> {
            if(i < 500){
                ringBitSet.setNextBit(true);
            }else{
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

}
