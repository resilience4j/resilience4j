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

import io.github.resilience4j.core.functions.CheckedSupplier;
import org.junit.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class CheckedFunctionUtilsTest {

    @Test
    public void shouldRecoverFromException() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new IOException("BAM!");
        };
        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.recover(callable, (ex) -> "Bla");

        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldRecoverFromSpecificExceptions() throws Throwable {
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
    public void shouldRecoverFromResult() throws Throwable {
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
    public void shouldRecoverFromException2() throws Throwable {
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
    public void shouldRecoverFromSpecificResult() throws Throwable {
        CheckedSupplier<String> supplier = () -> "Wrong Result";

        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.recover(supplier, (result) -> result.equals("Wrong Result"), (r) -> "Bla");
        String result = callableWithRecovery.get();

        assertThat(result).isEqualTo("Bla");
    }


    @Test(expected = RuntimeException.class)
    public void shouldRethrowException() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new IOException("BAM!");
        };
        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.recover(callable, (ex) -> {
            throw new RuntimeException();
        });

        callableWithRecovery.get();
    }

    @Test(expected = RuntimeException.class)
    public void shouldRethrowException2() throws Throwable {
        CheckedSupplier<String> callable = () -> {
            throw new RuntimeException("BAM!");
        };
        CheckedSupplier<String> callableWithRecovery = CheckedFunctionUtils.recover(callable, IllegalArgumentException.class, (ex) -> "Bla");

        callableWithRecovery.get();
    }
}
