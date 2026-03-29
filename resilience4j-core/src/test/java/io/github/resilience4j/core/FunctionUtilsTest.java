package io.github.resilience4j.core;

import java.io.IOException;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class FunctionUtilsTest {

    @Test
    void shouldChainFunctionAndResultHandler() {
        Function<String, String> function = ignored -> "foo";
        Function<String, String> functionWithRecovery = FunctionUtils.andThen(function, result -> "bar");

        String result = functionWithRecovery.apply("baz");

        assertThat(result).isEqualTo("bar");
    }


    @Test
    void shouldChainFunctionAndRecoverFromException() {
        Function<String, String> function = ignored -> {
            throw new RuntimeException("BAM!");
        };
        Function<String, String> functionWithRecovery = FunctionUtils
            .andThen(function, (result, ex) -> "foo");

        String result = functionWithRecovery.apply("bar");

        assertThat(result).isEqualTo("foo");
    }

    @Test
    void shouldChainFunctionAndRecoverWithErrorHandler() {
        Function<String, String> function = ignored -> {
            throw new RuntimeException("BAM!");
        };
        Function<String, String> functionWithRecovery = FunctionUtils
            .andThen(function, (result) -> result, ex -> "foo");

        String result = functionWithRecovery.apply("bar");

        assertThat(result).isEqualTo("foo");
    }

    @Test
    void shouldRecoverFunctionFromException() {
        Function<String, String> function = ignored -> {
            throw new RuntimeException("BAM!");
        };
        Function<String, String> functionWithRecovery = FunctionUtils.recover(function, (ex) -> "foo");

        String result = functionWithRecovery.apply("bar");

        assertThat(result).isEqualTo("foo");
    }

    @Test
    void shouldRecoverFunctionFromSpecificExceptions() {
        Function<String, String> function = ignored -> {
            throw new IllegalArgumentException("BAM!");
        };

        Function<String, String> functionWithRecovery = FunctionUtils.recover(function,
            asList(IllegalArgumentException.class, IOException.class),
            (ex) -> "foo");

        String result = functionWithRecovery.apply("bar");

        assertThat(result).isEqualTo("foo");
    }

    @Test
    void shouldRecoverFunctionFromSpecificResult() {
        Function<String, String> function = ignored -> "Wrong Result";

        Function<String, String> functionWithRecovery = FunctionUtils.recover(function, (result) -> result.equals("Wrong Result"), (r) -> "Correct Result");
        String result = functionWithRecovery.apply("foo");

        assertThat(result).isEqualTo("Correct Result");
    }

    @Test
    void shouldRethrowException() {
        Function<String, String> function = ignored -> {
                throw new IllegalArgumentException("BAM!");
            };
        Function<String, String> functionWithRecovery = FunctionUtils.recover(function, (ex) -> {
                throw new RuntimeException();
            });
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            functionWithRecovery.apply("foo"));
    }

    @Test
    void shouldRethrowException2() throws Exception {
        Function<String, String> function = ignored -> {
                throw new RuntimeException("BAM!");
            };
        Function<String, String> functionWithRecovery = FunctionUtils.recover(function, IllegalArgumentException.class, (ex) -> "foo");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            functionWithRecovery.apply("bar"));
    }
}
