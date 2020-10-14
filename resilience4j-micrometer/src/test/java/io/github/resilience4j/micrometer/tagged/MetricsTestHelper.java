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

import io.micrometer.core.instrument.Meter;

import java.util.Collection;
import java.util.Optional;

final class MetricsTestHelper {

    static <T extends Meter> Optional<T> findMeterByKindAndNameTags(Collection<T> meters, String kind,
        String name) {
        return meters.stream()
            .filter(g -> kind.equals(g.getId().getTag(TagNames.KIND)))
            .filter(g -> name.equals(g.getId().getTag(TagNames.NAME)))
            .findAny();
    }

    static <T extends Meter> Optional<T> findMeterByNamesTag(Collection<T> meters, String name) {
        return meters.stream()
            .filter(g -> name.equals(g.getId().getTag(TagNames.NAME)))
            .findAny();
    }
}
