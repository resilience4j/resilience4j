package io.github.resilience4j.utils;

import io.github.resilience4j.RecoveryTestService;
import io.github.resilience4j.recovery.DefaultRecoveryFunction;
import io.github.resilience4j.recovery.RecoveryFunction;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

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
}