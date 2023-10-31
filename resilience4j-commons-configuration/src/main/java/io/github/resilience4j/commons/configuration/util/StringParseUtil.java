/*
 *   Copyright 2023: Deepak Kumar
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

import io.github.resilience4j.commons.configuration.exception.ConfigParseException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class StringParseUtil {
    /**
     * Extracts the unique prefixes of the delimiter separated elements.
     *
     * @param iterator  the iterator
     * @param delimiter the delimiter
     * @return the unique prefixes
     */
    public static Set<String> extractUniquePrefixes(final Iterator<String> iterator, final String delimiter) {
        Set<String> uniquePrefixes = new HashSet<>();
        try{
            iterator.forEachRemaining(element -> {
                String prefix = element.substring(0, element.indexOf(delimiter));
                uniquePrefixes.add(prefix);
            });
            return uniquePrefixes;
        }catch (IndexOutOfBoundsException e){
            throw new ConfigParseException(String.format("Unable to extract prefix with delimiter: %s", delimiter), e);
        }
    }
}
