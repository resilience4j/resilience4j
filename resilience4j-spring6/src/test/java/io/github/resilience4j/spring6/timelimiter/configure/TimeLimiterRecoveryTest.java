package io.github.resilience4j.spring6.timelimiter.configure;

import io.github.resilience4j.spring6.TestApplication;
import io.github.resilience4j.spring6.TestDummyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestApplication.class)
class TimeLimiterRecoveryTest {
    @Autowired
    @Qualifier("timeLimiterDummyService")
    TestDummyService testDummyService;

    @Test
    void testAsyncRecovery() throws Exception {
        String result = testDummyService.async().toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("recovered");
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
