package io.github.resilience4j.core;

import org.junit.Test;

import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

public class CallableUtilsTest {

    @Test
    public void shouldChainSupplierAndRecoverFromException() throws Exception {

        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        //When
        Callable<String> supplierWithRecovery = CallableUtils.andThen(callable, (result, ex) -> "Bla");

        String result = supplierWithRecovery.call();

        //Then
        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldChainSupplierAndRecoverWithErrorHandler() throws Exception {

        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        //When
        Callable<String> supplierWithRecovery = CallableUtils.andThen(callable, (result) -> result, ex -> "Bla");

        String result = supplierWithRecovery.call();

        //Then
        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldRecoverSupplierFromException() throws Exception {

        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        //When
        Callable<String> supplierWithRecovery = CallableUtils.recover(callable, (ex) -> "Bla");

        String result = supplierWithRecovery.call();

        //Then
        assertThat(result).isEqualTo("Bla");
    }

    @Test(expected = WebServiceException.class)
    public void shouldRethrowException() throws Exception {

        Callable<String> callable = () -> {
            throw new IOException("BAM!");
        };
        //When
        Callable<String> supplierWithRecovery = CallableUtils.recover(callable, (ex) -> {
            throw new WebServiceException();
        });

        supplierWithRecovery.call();
    }
}
