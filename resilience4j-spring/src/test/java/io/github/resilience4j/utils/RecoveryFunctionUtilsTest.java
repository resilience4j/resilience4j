package io.github.resilience4j.utils;

import io.github.resilience4j.RecoveryTestService;
import io.github.resilience4j.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.recovery.RecoveryFunction;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class RecoveryFunctionUtilsTest {
    @Test
    public void givenDefaultRecoveryFunction_whenGetInstance_thenReturnsDefaultRecoveryFunctionInstance() throws Exception {
        RecoveryFunction recoveryFunction1 = RecoveryFunctionUtils.getInstance(DefaultRecoveryFunction.class);
        RecoveryFunction recoveryFunction2 = RecoveryFunctionUtils.getInstance(DefaultRecoveryFunction.class);

        assertThat(recoveryFunction1).isEqualTo(DefaultRecoveryFunction.getInstance());
        assertThat(recoveryFunction2).isEqualTo(DefaultRecoveryFunction.getInstance());
    }

    @Test
    public void givenRecoveryFunction_whenGewInstance_thenReturnsItsNewInstance() throws Exception {
        RecoveryFunction<String> recoveryFunction1 = RecoveryFunctionUtils.getInstance(RecoveryTestService.TestRecovery.class);
        RecoveryFunction<String> recoveryFunction2 = RecoveryFunctionUtils.getInstance(RecoveryTestService.TestRecovery.class);

        assertThat(recoveryFunction1).isInstanceOf(RecoveryTestService.TestRecovery.class);
        assertThat(recoveryFunction2).isInstanceOf(RecoveryTestService.TestRecovery.class);
        assertThat(recoveryFunction1).isNotEqualTo(recoveryFunction2);
    }

    @Test
    public void givenDefaultRecoveryFunctionAndFailingCompletableFuture_whenDecorateCompletionStage_thenReturnsException() throws Exception {
        CompletableFuture<String> failingCompletableFuture = new CompletableFuture<>();
        RuntimeException throwingException = new RuntimeException("Test");
        failingCompletableFuture.completeExceptionally(throwingException);

        CompletableFuture<String> decorated = RecoveryFunctionUtils.decorateCompletionStage(DefaultRecoveryFunction::getInstance, failingCompletableFuture).toCompletableFuture();

        assertThat(decorated.isCompletedExceptionally()).isTrue();
        assertThatThrownBy(() -> decorated.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCause(throwingException);
    }

    @Test
    public void givenRecoveryFunctionAndFailingCompletableFuture_whenDecorateCompletionStage_thenReturnsRecoveredCompletableFuture() throws Exception {
        CompletableFuture<String> failingCompletableFuture = new CompletableFuture<>();
        RuntimeException throwingException = new RuntimeException("Test");
        failingCompletableFuture.completeExceptionally(throwingException);

        CompletableFuture<String> decorated = RecoveryFunctionUtils.decorateCompletionStage(RecoveryTestService.TestRecovery::new, failingCompletableFuture).toCompletableFuture();

        assertThat(decorated.isCompletedExceptionally()).isFalse();
        assertThat(decorated.get(5, TimeUnit.SECONDS)).isEqualTo("recovered");
    }
}