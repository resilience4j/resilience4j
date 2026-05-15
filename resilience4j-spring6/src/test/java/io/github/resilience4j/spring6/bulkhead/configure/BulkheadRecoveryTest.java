/*
 * Copyright 2026 lespinsideg
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
package io.github.resilience4j.spring6.bulkhead.configure;

import io.github.resilience4j.spring6.TestApplication;
import io.github.resilience4j.spring6.TestDummyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class)
class BulkheadRecoveryTest {

    @Autowired
    @Qualifier("bulkheadDummyService")
    TestDummyService testDummyService;

    @Test
    void testRecovery() {
        assertThat(testDummyService.sync()).isEqualTo("recovered");
    }

    @Test
    void testAsyncRecovery() throws Exception {
        assertThat(testDummyService.async().toCompletableFuture().get(5, TimeUnit.SECONDS))
            .isEqualTo("recovered");
    }

    @Test
    void testAsyncThreadPoolRecovery() throws Exception {
        assertThat(
            testDummyService.asyncThreadPool().toCompletableFuture().get(5, TimeUnit.SECONDS))
            .isEqualTo("recovered");

        assertThat(testDummyService.asyncThreadPoolSuccess().toCompletableFuture()
            .get(5, TimeUnit.SECONDS)).isEqualTo("finished");
    }

    @Test
    void testMonoRecovery() {
        assertThat(testDummyService.mono("test").block()).isEqualTo("test");
    }

    @Test
    void testFluxRecovery() {
        assertThat(testDummyService.flux().blockFirst()).isEqualTo("recovered");
    }

    @Test
    void testObservableRecovery() {
        assertThat(testDummyService.observable().blockingFirst()).isEqualTo("recovered");
    }

    @Test
    void testSingleRecovery() {
        assertThat(testDummyService.single().blockingGet()).isEqualTo("recovered");
    }

    @Test
    void testCompletableRecovery() {
        assertThat(testDummyService.completable().blockingGet()).isNull();
    }

    @Test
    void testMaybeRecovery() {
        assertThat(testDummyService.maybe().blockingGet()).isEqualTo("recovered");
    }

    @Test
    void testFlowableRecovery() {
        assertThat(testDummyService.flowable().blockingFirst()).isEqualTo("recovered");
    }

    @Test
    void testRx3ObservableRecovery() {
        assertThat(testDummyService.rx3Observable().blockingFirst()).isEqualTo("recovered");
    }

    @Test
    void testRx3SingleRecovery() {
        assertThat(testDummyService.rx3Single().blockingGet()).isEqualTo("recovered");
    }

    @Test
    void testRx3CompletableRecovery() {
        testDummyService.rx3Completable().test().assertComplete();
    }

    @Test
    void testRx3MaybeRecovery() {
        assertThat(testDummyService.rx3Maybe().blockingGet()).isEqualTo("recovered");
    }

    @Test
    void testRx3FlowableRecovery() {
        assertThat(testDummyService.rx3Flowable().blockingFirst()).isEqualTo("recovered");
    }
}
