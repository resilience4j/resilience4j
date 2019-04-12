/*
 * Copyright 2019 Kyuhyen Hwang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.recovery;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

@SuppressWarnings({"WeakerAccess", "unused"})
public class RecoveryMethodTest {
    @Test
    public void recoverRuntimeExceptionTest() throws Throwable {
        RecoveryMethodTest target = new RecoveryMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        RecoveryMethod recoveryMethod = new RecoveryMethod("recovery", testMethod, new Object[]{"test"}, target);

        assertThat(recoveryMethod.recover(new RuntimeException("err"))).isEqualTo("recovered-RuntimeException");
    }

    @Test
    public void recoverIllegalArgumentExceptionTest() throws Throwable {
        RecoveryMethodTest target = new RecoveryMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        RecoveryMethod recoveryMethod = new RecoveryMethod("recovery", testMethod, new Object[]{"test"}, target);

        assertThat(recoveryMethod.recover(new IllegalArgumentException("err"))).isEqualTo("recovered-IllegalArgumentException");
    }

    @Test
    public void shouldThrowUnrecoverableThrowable() throws Throwable {
        RecoveryMethodTest target = new RecoveryMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        RecoveryMethod recoveryMethod = new RecoveryMethod("recovery", testMethod, new Object[]{"test"}, target);

        Throwable unrecoverableThrown = new Throwable("err");
        assertThatThrownBy(() -> recoveryMethod.recover(unrecoverableThrown)).isEqualTo(unrecoverableThrown);
    }

    @Test
    public void shouldCallPrivateRecoveryMethod() throws Throwable {
        RecoveryMethodTest target = new RecoveryMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        RecoveryMethod recoveryMethod = new RecoveryMethod("privateRecovery", testMethod, new Object[]{"test"}, target);

        assertThat(recoveryMethod.recover(new RuntimeException("err"))).isEqualTo("recovered-privateMethod");
    }

    @Test
    public void mismatchReturnType_shouldThrowNoSuchMethodException() throws Throwable {
        RecoveryMethodTest target = new RecoveryMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        assertThatThrownBy(() -> new RecoveryMethod("returnMismatchRecovery", testMethod, new Object[]{"test"}, target))
                .isInstanceOf(NoSuchMethodException.class)
                .hasMessage("class java.lang.String class io.github.resilience4j.recovery.RecoveryMethodTest.returnMismatchRecovery(class java.lang.String,class java.lang.Throwable)");
    }

    @Test
    public void notFoundRecoveryMethod_shouldThrowsNoSuchMethodException() throws Throwable {
        RecoveryMethodTest target = new RecoveryMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        assertThatThrownBy(() -> new RecoveryMethod("noMethod", testMethod, new Object[]{"test"}, target))
                .isInstanceOf(NoSuchMethodException.class)
                .hasMessage("class java.lang.String class io.github.resilience4j.recovery.RecoveryMethodTest.noMethod(class java.lang.String,class java.lang.Throwable)");
    }

    public String testMethod(String parameter) {
        return null;
    }

    public String recovery(String parameter, RuntimeException exception) {
        return "recovered-RuntimeException";
    }

    public String recovery(String parameter, IllegalArgumentException exception) {
        return "recovered-IllegalArgumentException";
    }

    public Object returnMismatchRecovery(String parameter, RuntimeException exception) {
        return "recovered";
    }

    private String privateRecovery(String parameter, RuntimeException exception) {
        return "recovered-privateMethod";
    }
}