/*
 *   Copyright 2026: Deepak Kumar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.github.resilience4j.commons.configuration.util;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StringParseUtilTest {
    @Test
    void extractUniquePrefixes() {
        Iterator<String> iterator = List.of("backendA.circuitBreaker.test1", "backendA.circuitBreaker.test2", "backendB.circuitBreaker.test3").iterator();

        Set<String> prefixes = StringParseUtil.extractUniquePrefixes(iterator, ".");

        assertThat(prefixes).containsExactlyInAnyOrder("backendA", "backendB");
    }
}