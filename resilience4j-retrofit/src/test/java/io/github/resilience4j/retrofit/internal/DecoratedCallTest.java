package io.github.resilience4j.retrofit.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import retrofit2.Call;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@RunWith(JUnit4.class)
public class DecoratedCallTest {

    private final Call<String> call = mock(StringCall.class);
    private final Call<String> decorated = new DecoratedCall<>(call);

    @Test
    public void passThroughCallsToDecoratedObject() throws IOException {
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

    @Test
    public void cloneShouldReturnDecoratedCall() {
        Call<String> clonedTarget = mock(StringCall.class);
        Mockito.when(call.clone()).thenReturn(clonedTarget);

        Call<String> clonedDecorator = decorated.clone();
        Mockito.verify(call).clone();
        assertThat(clonedDecorator).isInstanceOf(DecoratedCall.class);

        decorated.request();
        Mockito.verify(call).request();
        Mockito.verifyNoMoreInteractions(call);

        clonedDecorator.request();
        Mockito.verify(clonedTarget).request();
    }

    private interface StringCall extends Call<String> {
    }
}
