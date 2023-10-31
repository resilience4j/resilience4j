/*
 *  Copyright 2023 Mariusz Kopylec
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
 */
package io.github.resilience4j.micrometer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.ArrayList;
import java.util.List;

import static io.github.resilience4j.micrometer.tagged.TagNames.KIND;
import static io.github.resilience4j.micrometer.tagged.TagNames.NAME;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.BDDAssertions.then;

public class TimerAssertions {

    public static void thenSuccessTimed(MeterRegistry registry, Timer timer) {
        thenTimed(registry, timer, "successful", null);
    }

    public static void thenFailureTimed(MeterRegistry registry, Timer timer, Throwable throwable) {
        thenTimed(registry, timer, "failed", throwable);
    }

    private static void thenTimed(MeterRegistry registry, Timer timer, String resultKind, Throwable throwable) {
        List<Meter> meters = registry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(timer.getTimerConfig().getMetricNames()))
                .toList();
        then(meters.size()).isEqualTo(1);
        io.micrometer.core.instrument.Timer meter = (io.micrometer.core.instrument.Timer) meters.get(0);
        List<Tag> tags = timer.getTags().entrySet().stream().map(tag -> Tag.of(tag.getKey(), tag.getValue())).collect(toCollection(ArrayList::new));
        tags.add(Tag.of(NAME, timer.getName()));
        tags.add(Tag.of(KIND, resultKind));
        if (throwable != null) {
            tags.add(Tag.of("failure", timer.getTimerConfig().getOnFailureTagResolver().apply(throwable)));
        }
        then(meter.count()).isEqualTo(1);
        then(meter.getId().getTags()).containsExactlyInAnyOrderElementsOf(tags);
        registry.remove(meter);
    }
}
