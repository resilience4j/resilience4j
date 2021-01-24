package io.github.resilience4j.cache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class CacheRegistryStoreTest {

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

    @Before
    public void setupTest() {
        doReturn(CACHE_KEY).when(mutableEntryMock).getKey();
        doAnswer(invocation -> entryProcessorMock.process(mutableEntryMock, entryProcessorArgMock))
            .when(cacheStoreMock).invoke(eq(CACHE_KEY), any(EntryProcessor.class), any());

        classUnderTest = new CacheRegistryStore<>(cacheStoreMock);
    }

    @Test
    public void computeIfAbsent_cacheHit_mappingFunctionNotInvoked() {
        doReturn(1).when(mutableEntryMock).getValue();
        Function<String, Integer> mappingFunctionMock = Mockito.mock(Function.class);
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunctionMock;

        classUnderTest.computeIfAbsent(CACHE_KEY, mappingFunctionMock);

        verify(mappingFunctionMock, never()).apply(CACHE_KEY);
        verify(mutableEntryMock, never()).setValue(any());
    }

    @Test
    public void computeIfAbsent_cacheMiss_mappingFunctionInvoked() {
        Function<String, Integer> mappingFunction = k -> 7;
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        Integer cacheResult = classUnderTest.computeIfAbsent(CACHE_KEY, mappingFunction);

        verify(mutableEntryMock, times(1)).setValue(7);
        assertEquals(Integer.valueOf(7), cacheResult);
    }

    @Test
    public void computeIfAbsent_cacheMiss_mappingFunctionReturnsNull_returnOldValue() {
        Function<String, Integer> mappingFunction = k -> null;
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        Integer cacheResult = classUnderTest.computeIfAbsent(CACHE_KEY, mappingFunction);

        verify(mutableEntryMock, never()).setValue(any());
        assertNull(cacheResult);
    }

    @Test
    public void computeIfAbsent_cacheThrowsException_throwsUnwrappedEntryProcessorException() {
        Function<String, Integer> mappingFunction = s -> {
            throw new EntryProcessorException(new IllegalArgumentException());
        };
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        try {
            classUnderTest.computeIfAbsent(CACHE_KEY, mappingFunction);
            fail("Test should've thrown EntryProcessorException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void putIfAbsent_cacheHit_noCacheUpdate() {
        Function<String, Integer> mappingFunctionMock = Mockito.mock(Function.class);
        doReturn(36).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunctionMock;

        Integer cacheResult = classUnderTest.putIfAbsent(CACHE_KEY, 36);

        verify(mutableEntryMock, never()).setValue(any());
        assertEquals(Integer.valueOf(36), cacheResult);
    }

    @Test
    public void putIfAbsent_cacheMiss_updatesCache() {
        Function<String, Integer> mappingFunction = k -> 17;
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        Integer cacheResult = classUnderTest.putIfAbsent(CACHE_KEY, 17);

        verify(mutableEntryMock, times(1)).setValue(17);
        assertEquals(Integer.valueOf(17), cacheResult);
    }

    @Test
    public void putIfAbsent_cacheThrowsException_throwsUnwrappedEntryProcessorException() {
        Function<String, Integer> mappingFunction = s -> {
            throw new EntryProcessorException(new IllegalStateException());
        };
        doReturn(null).when(mutableEntryMock).getValue();
        entryProcessorMock = new CacheRegistryStore.AtomicComputeProcessor<>();
        entryProcessorArgMock = mappingFunction;

        try {
            classUnderTest.putIfAbsent(CACHE_KEY, 54);
            fail("Test should've thrown EntryProcessorException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }
}
