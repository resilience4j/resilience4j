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

import io.github.resilience4j.BulkheadDummyService;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class RecoveryMethodTest {
    @Test
    public void recoverTest() throws Throwable {
        BulkheadDummyService bulkheadDummyService = new BulkheadDummyService();
        Method monoMethod = bulkheadDummyService.getClass().getMethod("mono", String.class);
        RecoveryMethod recoveryMethod = new RecoveryMethod("monoRecovery", monoMethod, new Object[]{"test"}, bulkheadDummyService);

        Mono<String> recovered = (Mono<String>) recoveryMethod.recover(new RuntimeException("err"));

        assertThat(recovered.block()).isEqualTo("test");
    }

    @Test
    public void mismatchReturnType_shouldThrowClassCastException() throws Exception {
        BulkheadDummyService bulkheadDummyService = new BulkheadDummyService();
        Method observableMethod = bulkheadDummyService.getClass().getMethod("observable");

        assertThatThrownBy(() -> new RecoveryMethod("flowableRecovery", observableMethod, new Object[0], bulkheadDummyService))
                .isInstanceOf(ClassCastException.class)
                .hasMessage("recovery return type not matched (expected: io.reactivex.Observable, actual :io.reactivex.Flowable)");
    }

    @Test
    public void notFoundRecoveryMethod_shouldThrowsNoSuchMethodException() throws Exception {
        BulkheadDummyService bulkheadDummyService = new BulkheadDummyService();
        Method monoMethod = bulkheadDummyService.getClass().getMethod("mono", String.class);
        assertThatThrownBy(() -> new RecoveryMethod("noMethod", monoMethod, new Object[]{"test"}, bulkheadDummyService))
                .isInstanceOf(NoSuchMethodException.class)
                .hasMessage("class io.github.resilience4j.BulkheadDummyService.noMethod(class java.lang.String,class java.lang.Throwable)");

    }
}