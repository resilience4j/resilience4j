/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.circuitbreaker.configure;

import io.github.resilience4j.TestApplication;
import io.github.resilience4j.TestDummyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class)
public class CircuitBreakerRecoveryTest {

    @Autowired
    @Qualifier("circuitBreakerDummyService")
    TestDummyService testDummyService;

    @Test
    public void testRecovery() {
        assertThat(testDummyService.sync()).isEqualTo("recovered");
    }

    @Test
    public void testAsyncRecovery() throws Exception {
        assertThat(testDummyService.async().toCompletableFuture().get(5, TimeUnit.SECONDS))
            .isEqualTo("recovered");
    }

    @Test
    public void testMonoRecovery() {
        assertThat(testDummyService.mono("test").block()).isEqualTo("test");
    }

    @Test
    public void testFluxRecovery() {
        assertThat(testDummyService.flux().blockFirst()).isEqualTo("recovered");
    }

    @Test
    public void testObservableRecovery() {
        assertThat(testDummyService.observable().blockingFirst()).isEqualTo("recovered");
    }

    @Test
    public void testSingleRecovery() {
        assertThat(testDummyService.single().blockingGet()).isEqualTo("recovered");
    }

    @Test
    public void testCompletableRecovery() {
        assertThat(testDummyService.completable().blockingGet()).isNull();
    }

    @Test
    public void testMaybeRecovery() {
        assertThat(testDummyService.maybe().blockingGet()).isEqualTo("recovered");
    }

    @Test
    public void testFlowableRecovery() {
        assertThat(testDummyService.flowable().blockingFirst()).isEqualTo("recovered");
    }

    @Test
    public void testRx3ObservableRecovery() {
        assertThat(testDummyService.rx3Observable().blockingFirst()).isEqualTo("recovered");
    }

    @Test
    public void testRx3SingleRecovery() {
        assertThat(testDummyService.rx3Single().blockingGet()).isEqualTo("recovered");
    }

    @Test
    public void testRx3CompletableRecovery() {
        testDummyService.rx3Completable().test().assertComplete();
    }

    @Test
    public void testRx3MaybeRecovery() {
        assertThat(testDummyService.rx3Maybe().blockingGet()).isEqualTo("recovered");
    }

    @Test
    public void testRx3FlowableRecovery() {
        assertThat(testDummyService.rx3Flowable().blockingFirst()).isEqualTo("recovered");
    }
}
