package io.github.resilience4j.core;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SupplierUtilsTest {

    @Test
    void shouldChainSupplierAndResultHandler() {
        Supplier<String> supplier = () -> "BLA";
        Supplier<String> supplierWithRecovery = SupplierUtils.andThen(supplier, result -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldChainSupplierAndRecoverWithHandler() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils
            .andThen(supplier, (result, ex) -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldChainSupplierAndRecoverWithErrorHandler() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils
            .andThen(supplier, (result) -> result, ex -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }


    @Test
    void shouldRecoverSupplierFromException() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, (ex) -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverSupplierFromSpecificResult() {
        Supplier<String> supplier = () -> "Wrong Result";

        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, (result) -> result.equals("Wrong Result"), (r) -> "Bla");
        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverSupplierFromSpecificException() {
        Supplier<String> supplier = () -> {
            throw new IllegalArgumentException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, RuntimeException.class, (ex) -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverSupplierFromSpecificExceptions() {
        Supplier<String> supplier = () -> {
            throw new IllegalArgumentException("BAM!");
        };

        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier,
            asList(IllegalArgumentException.class, IOException.class),
            (ex) -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRethrowException() {
        Supplier<String> supplier = () -> {
                throw new RuntimeException("BAM!");
            };
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, (ex) -> {
                throw new RuntimeException();
            });
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            supplierWithRecovery.get());
    }

    @Test
    void shouldRethrowException2() {
        Supplier<String> supplier = () -> {
                throw new RuntimeException("BAM!");
            };
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, IllegalArgumentException.class, (ex) -> "Bla");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            supplierWithRecovery.get());
    }
}
