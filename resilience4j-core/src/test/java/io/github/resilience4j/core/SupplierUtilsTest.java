package io.github.resilience4j.core;

import org.junit.Test;

import java.io.IOException;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class SupplierUtilsTest {

    @Test
    public void shouldChainSupplierAndResultHandler() {
        Supplier<String> supplier = () -> "BLA";
        Supplier<String> supplierWithRecovery = SupplierUtils.andThen(supplier, result -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldChainSupplierAndRecoverWithHandler() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils
            .andThen(supplier, (result, ex) -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldChainSupplierAndRecoverWithErrorHandler() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils
            .andThen(supplier, (result) -> result, ex -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }


    @Test
    public void shouldRecoverSupplierFromException() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, (ex) -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldRecoverSupplierFromSpecificResult() {
        Supplier<String> supplier = () -> "Wrong Result";

        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, (result) -> result.equals("Wrong Result"), (r) -> "Bla");
        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldRecoverSupplierFromSpecificException() {
        Supplier<String> supplier = () -> {
            throw new IllegalArgumentException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, RuntimeException.class, (ex) -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldRecoverSupplierFromSpecificExceptions() {
        Supplier<String> supplier = () -> {
            throw new IllegalArgumentException("BAM!");
        };

        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier,
            asList(IllegalArgumentException.class, IOException.class),
            (ex) -> "Bla");

        String result = supplierWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test(expected = RuntimeException.class)
    public void shouldRethrowException() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, (ex) -> {
            throw new RuntimeException();
        });

        supplierWithRecovery.get();
    }

    @Test(expected = RuntimeException.class)
    public void shouldRethrowException2() {
        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, IllegalArgumentException.class, (ex) -> "Bla");

        supplierWithRecovery.get();
    }
}
