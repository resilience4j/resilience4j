/*
 *
 *  Copyright 2018 Valtteri Walldén
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
package io.github.resilience4j.prometheus;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.internal.InMemoryBulkheadRegistry;
import io.prometheus.client.CollectorRegistry;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.Map;
import org.junit.Test;

import java.util.function.Supplier;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class BulkheadExportsTest {

  @Test
  public void testExportsCircuitBreakerStates() {
      // Given
      final CollectorRegistry registry = new CollectorRegistry();

      final Bulkhead bulkhead = Bulkhead.ofDefaults("foo");

      BulkheadExports.ofIterable("boo_bulkhead", singletonList(bulkhead)).register(registry);

      final Supplier<Map<String, Double>> values = () -> HashSet
              .of("available_concurrent_calls")
              .map(param ->
                      Tuple.of(param, registry.getSampleValue(
                              "boo_bulkhead",
                              new String[]{"name", "param"},
                              new String[]{"foo", param})))
              .toMap(t -> t);

      // When
      final Map<String, Double> initialValues = values.get();

      // Then
      assertThat(initialValues).isEqualTo(HashMap.of("available_concurrent_calls", 25.0));
  }

    @Test
    public void testConstructors() {
        final BulkheadRegistry registry = new InMemoryBulkheadRegistry(BulkheadConfig.ofDefaults());

        assertThat(BulkheadExports.ofIterable("boo_bulkheads", singleton(Bulkhead.ofDefaults("foo")))).isNotNull();
        assertThat(BulkheadExports.ofBulkheadRegistry("boo_bulkheads", registry)).isNotNull();
        assertThat(BulkheadExports.ofSupplier("boo_bulkheads", () -> singleton(Bulkhead.ofDefaults("foo")))).isNotNull();

        assertThat(BulkheadExports.ofIterable(singleton(Bulkhead.ofDefaults("foo")))).isNotNull();
        assertThat(BulkheadExports.ofBulkheadRegistry(registry)).isNotNull();
        assertThat(BulkheadExports.ofSupplier(() -> singleton(Bulkhead.ofDefaults("foo")))).isNotNull();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullName() {
        BulkheadExports.ofSupplier(null, () -> singleton(Bulkhead.ofDefaults("foo")));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullSupplier() {
        BulkheadExports.ofSupplier("boo_bulkheads", null);
    }
}
