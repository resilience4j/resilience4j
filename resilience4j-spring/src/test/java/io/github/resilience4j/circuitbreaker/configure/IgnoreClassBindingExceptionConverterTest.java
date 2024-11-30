package io.github.resilience4j.circuitbreaker.configure;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class IgnoreClassBindingExceptionConverterTest {


    private final String NON_EXISTENT_EXCEPTION = "com.example.NonExistentException";

    private IgnoreClassBindingExceptionConverter shouldIgnoreUnknownExceptionConverter;
    private IgnoreClassBindingExceptionConverter shouldNotIgnoreUnknownExceptionConverter;

    @Before
    public void setUp() {
        shouldIgnoreUnknownExceptionConverter = new IgnoreClassBindingExceptionConverter(true);
        shouldNotIgnoreUnknownExceptionConverter = new IgnoreClassBindingExceptionConverter(false);
    }

    @Test
    public void testConvertValidExceptionClass() {
        Class<? extends Throwable> result = shouldIgnoreUnknownExceptionConverter.convert("java.lang.NullPointerException");
        assertThat(result).isEqualTo(NullPointerException.class);
    }

    @Test
    public void testConvertUnknownExceptionClass_IgnoreUnknownTrue() {
        Class<? extends Throwable> result = shouldIgnoreUnknownExceptionConverter.convert(NON_EXISTENT_EXCEPTION);
        assertThat(result).isEqualTo(IgnoreClassBindingExceptionConverter.PlaceHolderException.class);
    }

    @Test
    public void testConvertUnknownExceptionClass_IgnoreUnknownFalse() {
        assertThatThrownBy(() -> shouldNotIgnoreUnknownExceptionConverter.convert(NON_EXISTENT_EXCEPTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Class not found: " + NON_EXISTENT_EXCEPTION);
    }

    @Test
    public void testConvertValidExceptionClassWithoutIgnoreUnknown() {
        Class<? extends Throwable> result = shouldNotIgnoreUnknownExceptionConverter.convert("java.lang.IllegalArgumentException");
        assertThat(result).isEqualTo(IllegalArgumentException.class);
    }
}
