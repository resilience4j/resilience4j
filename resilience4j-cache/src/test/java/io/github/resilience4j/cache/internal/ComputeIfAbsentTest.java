package io.github.resilience4j.cache.internal;

import io.github.resilience4j.cache.internal.CacheImpl.ComputeIfAbsent;
import io.github.resilience4j.core.functions.CheckedSupplier;
import org.junit.Before;
import org.junit.Test;

import javax.cache.processor.MutableEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class ComputeIfAbsentTest {

    private MutableEntry<String, String> mockEntry;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mockEntry = mock(MutableEntry.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnCachedValueIfCached() {
        given(mockEntry.exists()).willReturn(true);
        given(mockEntry.getValue()).willReturn("Cached Value");
        CheckedSupplier<String> mockedSupplier = mock(CheckedSupplier.class);

        ComputeIfAbsent<String, String> processor = new ComputeIfAbsent<>(mockedSupplier);

        assertThat(processor.process(mockEntry)).isEqualTo("Cached Value");
        verifyNoInteractions(mockedSupplier);
    }

    @Test
    public void shouldReturnSuppliedValueIfNotCached() throws Throwable {
        given(mockEntry.exists()).willReturn(false);
        CheckedSupplier<String> supplier = spy(new SpyableSupplier("Supplied Value"));

        ComputeIfAbsent<String, String> processor = new ComputeIfAbsent<>(supplier);

        assertThat(processor.process(mockEntry)).isEqualTo("Supplied Value");
        verify(supplier).get();
        verify(mockEntry).setValue("Supplied Value");
    }

    @Test
    public void shouldRethrowSupplierException() {
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
