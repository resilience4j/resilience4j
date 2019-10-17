/*
 * Copyright 2019 Yevhenii Voievodin
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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;

import java.util.Collection;
import java.util.Optional;

final class MetricsTestHelper {

    static Optional<Gauge> findGaugeByKindAndNameTags(Collection<Gauge> gauges, String kind,
        String name) {
        return gauges.stream()
            .filter(g -> kind.equals(g.getId().getTag(TagNames.KIND)))
            .filter(g -> name.equals(g.getId().getTag(TagNames.NAME)))
            .findAny();
    }

    static Optional<Timer> findTimerByKindAndNameTags(Collection<Timer> timers, String kind,
        String name) {
        return timers.stream()
            .filter(g -> kind.equals(g.getId().getTag(TagNames.KIND)))
            .filter(g -> name.equals(g.getId().getTag(TagNames.NAME)))
            .findAny();
    }

    static Optional<Counter> findCounterByKindAndNameTags(Collection<Counter> counters, String kind,
        String name) {
        return counters.stream()
            .filter(g -> kind.equals(g.getId().getTag(TagNames.KIND)))
            .filter(g -> name.equals(g.getId().getTag(TagNames.NAME)))
            .findAny();
    }

    static Optional<Counter> findCounterByNamesTag(Collection<Counter> gauges, String name) {
        return gauges.stream()
            .filter(g -> name.equals(g.getId().getTag(TagNames.NAME)))
            .findAny();
    }

    static Optional<Gauge> findGaugeByNamesTag(Collection<Gauge> gauges, String name) {
        return gauges.stream()
            .filter(g -> name.equals(g.getId().getTag(TagNames.NAME)))
            .findAny();
    }
}
