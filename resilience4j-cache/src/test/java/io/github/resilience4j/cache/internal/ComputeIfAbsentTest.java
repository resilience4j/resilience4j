/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.cache.internal;

import io.github.resilience4j.cache.internal.CacheImpl.ComputeIfAbsent;
import io.github.resilience4j.core.functions.CheckedSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.cache.processor.MutableEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ComputeIfAbsentTest {

    private MutableEntry<String, String> mockEntry;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        mockEntry = mock(MutableEntry.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldReturnCachedValueIfCached() {
        given(mockEntry.exists()).willReturn(true);
        given(mockEntry.getValue()).willReturn("Cached Value");
        CheckedSupplier<String> mockedSupplier = mock(CheckedSupplier.class);

        ComputeIfAbsent<String, String> processor = new ComputeIfAbsent<>(mockedSupplier);

        assertThat(processor.process(mockEntry)).isEqualTo("Cached Value");
        verifyNoInteractions(mockedSupplier);
    }

    @Test
    void shouldReturnSuppliedValueIfNotCached() throws Throwable {
        given(mockEntry.exists()).willReturn(false);
        CheckedSupplier<String> supplier = spy(new SpyableSupplier("Supplied Value"));

        ComputeIfAbsent<String, String> processor = new ComputeIfAbsent<>(supplier);

        assertThat(processor.process(mockEntry)).isEqualTo("Supplied Value");
        verify(supplier).get();
        verify(mockEntry).setValue("Supplied Value");
    }

    @Test
    void shouldRethrowSupplierException() {
        given(mockEntry.exists()).willReturn(false);
        CheckedSupplier<String> supplier = () -> {
            throw new RuntimeException("Boom!");
        };

        ComputeIfAbsent<String, String> processor = new ComputeIfAbsent<>(supplier);

        assertThatThrownBy(() -> processor.process(mockEntry)).isInstanceOf(RuntimeException.class).hasMessage("Boom!");
    }

    private static class SpyableSupplier implements CheckedSupplier<String> {

        private final String suppliedValue;

        SpyableSupplier(String suppliedValue) {
            this.suppliedValue = suppliedValue;
        }

        @Override
        public String get() {
            return suppliedValue;
        }
    }
}
