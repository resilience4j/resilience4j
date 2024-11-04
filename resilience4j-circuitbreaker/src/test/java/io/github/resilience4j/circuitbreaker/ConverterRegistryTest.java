package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.core.Converter;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;


public class ConverterRegistryTest {

    private ConverterRegistry registry;

    @Before
    public void setUp() {
        this.registry = new ConverterRegistry();
    }

    @Test
    public void testRegisterConverter() {
        Converter<String, Integer> stringToIntegerConverter = Integer::parseInt;
        registry.registerConverter(String.class, stringToIntegerConverter);

        Converter<?, ?> converter = registry.getConverter(String.class);

        assertThat(converter).isNotNull();
        assertThat(converter).isInstanceOf(Converter.class);
    }

    @Test
    public void testConvert() {
        Converter<String, Integer> stringToIntegerConverter = Integer::parseInt;
        registry.registerConverter(String.class, stringToIntegerConverter);

        Integer result = registry.convert("123", Integer.class);
        assertThat(result).isEqualTo(123);
    }

    @Test
    public void testRemoveConverter() {
        Converter<String, Integer> stringToIntegerConverter = Integer::parseInt;
        registry.registerConverter(String.class, stringToIntegerConverter);

        assertThat(registry.getConverter(String.class)).isNotNull();
        registry.removeConverter(String.class);
        assertThat(registry.getConverter(String.class)).isNull();
    }

    @Test
    public void testConvertThrowsExceptionForUnregisteredType() {
       assertThatThrownBy(() -> registry.convert("123", Integer.class))
               .isInstanceOf(IllegalArgumentException.class)
               .hasMessageContaining("No converter registered for class java.lang.String");
    }
}
