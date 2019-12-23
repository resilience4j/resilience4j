package io.github.resilience4j.core;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

public class CallableUtilsTest {

    @Test
    public void shouldChainCallableAndResultHandler() throws Exception {
        Callable<String> supplier = () -> "BLA";
        Callable<String> callableWithRecovery = CallableUtils.andThen(supplier, result -> "Bla");

        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }


    @Test
    public void shouldChainCallableAndRecoverFromException() throws Exception {
        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        Callable<String> callableWithRecovery = CallableUtils
            .andThen(callable, (result, ex) -> "Bla");

        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldChainCallableAndRecoverWithErrorHandler() throws Exception {
        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        Callable<String> callableWithRecovery = CallableUtils
            .andThen(callable, (result) -> result, ex -> "Bla");

        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldRecoverCallableFromException() throws Exception {
        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        Callable<String> callableWithRecovery = CallableUtils.recover(callable, (ex) -> "Bla");

        String result = callableWithRecovery.call();

        assertThat(result).isEqualTo("Bla");
    }

    @Test(expected = RuntimeException.class)
    public void shouldRethrowException() throws Exception {
        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        Callable<String> callableWithRecovery = CallableUtils.recover(callable, (ex) -> {
            throw new RuntimeException();
        });

        callableWithRecovery.call();
    }
}
