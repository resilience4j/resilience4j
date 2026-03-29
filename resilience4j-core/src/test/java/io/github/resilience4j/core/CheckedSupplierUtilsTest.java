package io.github.resilience4j.core;

import io.github.resilience4j.core.functions.CheckedSupplier;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CheckedSupplierUtilsTest {

    @Test
    void shouldRecoverFromException() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new IOException("BAM!");
        };
        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.recover(callable, (ex) -> "Bla");

        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverFromSpecificExceptions() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new IOException("BAM!");
        };

        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.recover(callable,
            asList(IllegalArgumentException.class, IOException.class),
            (ex) -> "Bla");

        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverFromResult() throws Throwable {
        CheckedSupplier<String> callable = () -> "Wrong Result";

        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.andThen(callable, (result, ex) -> {
            if(result.equals("Wrong Result")){
                return "Bla";
            }
            return result;
        });

        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverFromException2() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new IllegalArgumentException("BAM!");
        };
        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.andThen(callable, (result, ex) -> {
            if(ex instanceof IllegalArgumentException){
                return "Bla";
            }
            return result;
        });

        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverFromSpecificResult() throws Throwable {
        CheckedSupplier<String> supplier = () -> "Wrong Result";

        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.recover(supplier, (result) -> result.equals("Wrong Result"), (r) -> "Bla");
        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }


    @Test
    void shouldRethrowException() throws Throwable {
        CheckedSupplier<String> callable = () -> {
                throw new IOException("BAM!");
            };
        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.recover(callable, (ex) -> {
                throw new RuntimeException();
            });
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            callableWithRecovery.get());
    }

    @Test
    void shouldRethrowException2() throws Throwable {
        CheckedSupplier<String> callable = () -> {
                throw new RuntimeException("BAM!");
            };
        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.recover(callable, IllegalArgumentException.class, (ex) -> "Bla");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            callableWithRecovery.get());
    }
}
