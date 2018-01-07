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

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.Test;

public class BitSetModTest {
    public static final int CAPACITY = 5;

    @Test
    public void get() {
        BitSetMod source = new BitSetMod(CAPACITY);
        for (int i = 0; i < CAPACITY; i++) {
            then(source.get(i)).isFalse();
        }
        source.set(0, true);
        source.set(2, true);
        source.set(4, true);
        for (int i = 0; i < CAPACITY; i++) {
            if (i % 2 == 0) {
                then(source.get(i)).isTrue();
            } else {
                then(source.get(i)).isFalse();
            }
        }
    }
}
