package io.github.resilience4j.core;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CallableUtilsTest {

    @Test
    void shouldChainCallableAndResultHandler() throws Exception {
        Callable<String> Callable = () -> "BLA";
        Callable<String> callableWithRecovery = CallableUtils.andThen(Callable, result -> "Bla");

        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }


    @Test
    void shouldChainCallableAndRecoverFromException() throws Exception {
        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        Callable<String> callableWithRecovery = CallableUtils
            .andThen(callable, (result, ex) -> "Bla");

        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldChainCallableAndRecoverWithErrorHandler() throws Exception {
        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        Callable<String> callableWithRecovery = CallableUtils
            .andThen(callable, (result) -> result, ex -> "Bla");

        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverCallableFromException() throws Exception {
        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        Callable<String> callableWithRecovery = CallableUtils.recover(callable, (ex) -> "Bla");

        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverCallableFromSpecificExceptions() throws Exception {
        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };

        Callable<String> callableWithRecovery = CallableUtils.recover(callable,
            asList(IllegalArgumentException.class, IOException.class),
            (ex) -> "Bla");

        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverCallableFromSpecificResult() throws Exception {
        Callable<String> supplier = () -> "Wrong Result";

        Callable<String> callableWithRecovery = CallableUtils.recover(supplier, (result) -> result.equals("Wrong Result"), (r) -> "Bla");
        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRethrowException() throws Exception {
        Callable<String> callable = () -> {
                throw new IOException("BAM!");
            };
        Callable<String> callableWithRecovery = CallableUtils.recover(callable, (ex) -> {
                throw new RuntimeException();
            });
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            callableWithRecovery.call());
    }

    @Test
    void shouldRethrowException2() throws Exception {
        Callable<String> callable = () -> {
                throw new RuntimeException("BAM!");
            };
        Callable<String> callableWithRecovery = CallableUtils.recover(callable, IllegalArgumentException.class, (ex) -> "Bla");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            callableWithRecovery.call());
    }
}
