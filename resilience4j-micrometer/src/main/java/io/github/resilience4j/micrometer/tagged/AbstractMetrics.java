/*
 * Copyright 2019 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micrometer.tagged;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

abstract class AbstractMetrics {

    protected ConcurrentMap<String, Set<Meter.Id>> meterIdMap;

    AbstractMetrics() {
        // Using ConcurrentHashMap for virtual thread optimization - simple meter ID tracking
        this.meterIdMap = new ConcurrentHashMap<>();
    }

    void removeMetrics(MeterRegistry registry, String name) {
        Set<Meter.Id> ids = meterIdMap.get(name);
        if (ids != null) {
            ids.forEach(registry::remove);
        }
        meterIdMap.remove(name);
    }

    List<Tag> mapToTagsList(Map<String, String> tagsMap) {
        return tagsMap.entrySet()
                .stream().map(tagsEntry -> Tag.of(tagsEntry.getKey(), tagsEntry.getValue()))
                .collect(Collectors.toList());
    }
}
