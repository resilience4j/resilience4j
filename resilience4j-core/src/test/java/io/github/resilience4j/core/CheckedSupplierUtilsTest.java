package io.github.resilience4j.core;

import io.github.resilience4j.core.functions.CheckedSupplier;
import org.junit.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class CheckedSupplierUtilsTest {

    @Test
    public void shouldRecoverFromException() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new IOException("BAM!");
        };
        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.recover(callable, (ex) -> "Bla");

        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldRecoverFromSpecificExceptions() throws Throwable {
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
    public void shouldRecoverFromResult() throws Throwable {
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
    public void shouldRecoverFromException2() throws Throwable {
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
    public void shouldRecoverFromSpecificResult() throws Throwable {
        CheckedSupplier<String> supplier = () -> "Wrong Result";

        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.recover(supplier, (result) -> result.equals("Wrong Result"), (r) -> "Bla");
        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }


    @Test(expected = RuntimeException.class)
    public void shouldRethrowException() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new IOException("BAM!");
        };
        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.recover(callable, (ex) -> {
            throw new RuntimeException();
        });

        callableWithRecovery.get();
    }

    @Test(expected = RuntimeException.class)
    public void shouldRethrowException2() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new RuntimeException("BAM!");
        };
        CheckedSupplier<String> callableWithRecovery = CheckedSupplierUtils.recover(callable, IllegalArgumentException.class, (ex) -> "Bla");

        callableWithRecovery.get();
    }
}
