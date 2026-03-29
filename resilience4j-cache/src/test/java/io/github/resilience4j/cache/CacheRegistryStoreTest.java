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
package io.github.resilience4j.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class CacheRegistryStoreTest {

    private static final String CACHE_KEY = "testKey";

    @Mock
    private Cache<String, Integer> cacheStoreMock;

    @Mock
    private EntryProcessor<String, Integer, Integer> entryProcessorMock;

    @Mock
    private MutableEntry<String, Integer> mutableEntryMock;

    @Mock
    private Object entryProcessorArgMock;

    private CacheRegistryStore<Integer> classUnderTest;

    @BeforeEach
    void setupTest() {
        lenient().doReturn(CACHE_KEY).when(mutableEntryMock).getKey();
        lenient().doAnswer(invocation -> entryProcessorMock.process(mutableEntryMock, entryProcessorArgMock))
            .when(cacheStoreMock).invoke(eq(CACHE_KEY), any(EntryProcessor.class), any());

        classUnderTest = new CacheRegistryStore<>(cacheStoreMock);
    }

    @Test
    void computeIfAbsent_cacheHit_mappingFunctionNotInvoked() {
        doReturn(1).when(mutableEntryMock).getValue();
        Function<String, Integer> mappingFunctionMock = Mockito.mock(Function.class);
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunctionMock;

        classUnderTest.computeIfAbsent(CACHE_KEY, mappingFunctionMock);

        verify(mappingFunctionMock, never()).apply(CACHE_KEY);
        verify(mutableEntryMock, never()).setValue(any());
    }

    @Test
    void computeIfAbsent_cacheMiss_mappingFunctionInvoked() {
        Function<String, Integer> mappingFunction = k -> 7;
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        Integer cacheResult = classUnderTest.computeIfAbsent(CACHE_KEY, mappingFunction);

        verify(mutableEntryMock).setValue(7);
        assertThat(cacheResult).isEqualTo(Integer.valueOf(7));
    }

    @Test
    void computeIfAbsent_cacheMiss_mappingFunctionReturnsNull_returnOldValue() {
        Function<String, Integer> mappingFunction = k -> null;
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        Integer cacheResult = classUnderTest.computeIfAbsent(CACHE_KEY, mappingFunction);

        verify(mutableEntryMock, never()).setValue(any());
        assertThat(cacheResult).isNull();
    }

    @Test
    void computeIfAbsent_cacheThrowsException_throwsUnwrappedEntryProcessorException() {
        Function<String, Integer> mappingFunction = s -> {
            throw new EntryProcessorException(new IllegalArgumentException());
        };
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        assertThatThrownBy(() -> classUnderTest.computeIfAbsent(CACHE_KEY, mappingFunction))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void putIfAbsent_cacheHit_noCacheUpdate() {
        Function<String, Integer> mappingFunctionMock = Mockito.mock(Function.class);
        doReturn(36).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunctionMock;

        Integer cacheResult = classUnderTest.putIfAbsent(CACHE_KEY, 36);

        verify(mutableEntryMock, never()).setValue(any());
        assertThat(cacheResult).isEqualTo(Integer.valueOf(36));
    }

    @Test
    void putIfAbsent_cacheMiss_updatesCache() {
        Function<String, Integer> mappingFunction = k -> 17;
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        Integer cacheResult = classUnderTest.putIfAbsent(CACHE_KEY, 17);

        verify(mutableEntryMock).setValue(17);
        assertThat(cacheResult).isEqualTo(Integer.valueOf(17));
    }

    @Test
    void putIfAbsent_cacheThrowsException_throwsUnwrappedEntryProcessorException() {
        Function<String, Integer> mappingFunction = s -> {
            throw new EntryProcessorException(new IllegalStateException());
        };
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        assertThatThrownBy(() -> classUnderTest.putIfAbsent(CACHE_KEY, 54))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
    }
}
