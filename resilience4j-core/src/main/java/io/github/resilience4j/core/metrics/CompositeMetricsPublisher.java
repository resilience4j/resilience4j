/*
 * Copyright 2019 Ingyu Hwang
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

package io.github.resilience4j.core.metrics;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class CompositeMetricsPublisher<E> implements MetricsPublisher<E> {

    private final List<MetricsPublisher<E>> delegates;

    public CompositeMetricsPublisher() {
        this.delegates = new ArrayList<>();
    }

    public CompositeMetricsPublisher(List<MetricsPublisher<E>> delegates) {
        this.delegates = new ArrayList<>(requireNonNull(delegates));
    }

    public void addMetricsPublisher(MetricsPublisher<E> metricsPublisher) {
        delegates.add(requireNonNull(metricsPublisher));
    }

    @Override
    public void publishMetrics(E entry) {
        delegates.forEach(publisher -> publisher.publishMetrics(entry));
    }

    @Override
    public void removeMetrics(E entry) {
        delegates.forEach(publisher -> publisher.removeMetrics(entry));
    }

}
