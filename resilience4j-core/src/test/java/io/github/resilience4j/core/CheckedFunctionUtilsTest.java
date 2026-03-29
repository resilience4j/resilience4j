/*
 *
 *  Copyright 2020: Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.core;

import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.core.functions.CheckedSupplier;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CheckedFunctionUtilsTest {

    @Test
    void shouldRecoverFromException() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new IOException("BAM!");
        };
        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.recover(callable, (ex) -> "Bla");

        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverFromSpecificExceptions() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new IOException("BAM!");
        };

        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.recover(callable,
            asList(IllegalArgumentException.class, IOException.class),
            (ex) -> "Bla");

        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    void shouldRecoverFromResult() throws Throwable {
        CheckedSupplier<String> callable = () -> "Wrong Result";

        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.andThen(callable, (result, ex) -> {
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
        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.andThen(callable, (result, ex) -> {
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

        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.recover(supplier, (result) -> result.equals("Wrong Result"), (r) -> "Bla");
        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }


    @Test
    void shouldRethrowException() throws Throwable {
        CheckedSupplier<String> callable = () -> {
                throw new IOException("BAM!");
            };
        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.recover(callable, (ex) -> {
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
        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.recover(callable, IllegalArgumentException.class, (ex) -> "Bla");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            callableWithRecovery.get());
    }

    @Test
    void shouldChainCheckedFunctionAndResultHandler() throws Throwable {
        CheckedFunction<String, String> function = ignored -> "foo";
        CheckedFunction<String, String> functionWithRecovery = CheckedFunctionUtils.andThen(function, result -> "bar");

        String result = functionWithRecovery.apply("baz");

        assertThat(result).isEqualTo("bar");
    }


    @Test
    void shouldChainCheckedFunctionAndRecoverFromException() throws Throwable {
        CheckedFunction<String, String> function = ignored -> {
            throw new RuntimeException("BAM!");
        };
        CheckedFunction<String, String> functionWithRecovery = CheckedFunctionUtils
                .andThen(function, (result, ex) -> "foo");

        String result = functionWithRecovery.apply("bar");

        assertThat(result).isEqualTo("foo");
    }

    @Test
    void shouldChainCheckedFunctionAndRecoverWithErrorHandler() throws Throwable {
        CheckedFunction<String, String> function = ignored -> {
            throw new RuntimeException("BAM!");
        };
        CheckedFunction<String, String> functionWithRecovery = CheckedFunctionUtils
                .andThen(function, (result) -> result, ex -> "foo");

        String result = functionWithRecovery.apply("bar");

        assertThat(result).isEqualTo("foo");
    }

    @Test
    void shouldRecoverCheckedFunctionFromException() throws Throwable {
        CheckedFunction<String, String> function = ignored -> {
            throw new RuntimeException("BAM!");
        };
        CheckedFunction<String, String> functionWithRecovery = CheckedFunctionUtils.recover(function, (ex) -> "foo");

        String result = functionWithRecovery.apply("bar");

        assertThat(result).isEqualTo("foo");
    }

    @Test
    void shouldRecoverCheckedFunctionFromSpecificExceptions() throws Throwable {
        CheckedFunction<String, String> function = ignored -> {
            throw new IllegalArgumentException("BAM!");
        };

        CheckedFunction<String, String> functionWithRecovery = CheckedFunctionUtils.recover(function,
                asList(IllegalArgumentException.class, IOException.class),
                (ex) -> "foo");

        String result = functionWithRecovery.apply("bar");

        assertThat(result).isEqualTo("foo");
    }

    @Test
    void shouldRecoverCheckedFunctionFromSpecificResult() throws Throwable {
        CheckedFunction<String, String> function = ignored -> "Wrong Result";

        CheckedFunction<String, String> functionWithRecovery = CheckedFunctionUtils.recover(function, (result) -> result.equals("Wrong Result"), (r) -> "Correct Result");
        String result = functionWithRecovery.apply("foo");

        assertThat(result).isEqualTo("Correct Result");
    }

    @Test
    void shouldRethrowCheckedFunctionException() throws Throwable {
        CheckedFunction<String, String> function = ignored -> {
                throw new IllegalArgumentException("BAM!");
            };
        CheckedFunction<String, String> functionWithRecovery = CheckedFunctionUtils.recover(function, (ex) -> {
                throw new RuntimeException();
            });
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            functionWithRecovery.apply("foo"));
    }

    @Test
    void shouldRethrowCheckedFuntctionException2() throws Throwable {
        CheckedFunction<String, String> function = ignored -> {
                throw new RuntimeException("BAM!");
            };
        CheckedFunction<String, String> functionWithRecovery = CheckedFunctionUtils.recover(function, IllegalArgumentException.class, (ex) -> "foo");
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->

            functionWithRecovery.apply("bar"));
    }
}
