package io.github.resilience4j.retrofit.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import retrofit2.Call;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class DecoratedCallTest {

    @Test
    public void passThroughCallsToDecoratedObject() throws IOException {
        final Call<String> call = mock(StringCall.class);
        final Call<String> decorated = new DecoratedCall<>(call);

        decorated.cancel();
        Mockito.verify(call).cancel();

        decorated.enqueue(null);
        Mockito.verify(call).enqueue(any());

        decorated.isExecuted();
        Mockito.verify(call).isExecuted();

        decorated.isCanceled();
        Mockito.verify(call).isCanceled();

        decorated.clone();
        Mockito.verify(call).clone();

        decorated.request();
        Mockito.verify(call).request();

        decorated.execute();
        Mockito.verify(call).execute();
    }

    private interface StringCall extends Call<String> {
    }
}
