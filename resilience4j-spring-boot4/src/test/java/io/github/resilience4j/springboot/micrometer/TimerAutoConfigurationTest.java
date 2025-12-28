package io.github.resilience4j.springboot.micrometer;

import io.github.resilience4j.common.micrometer.monitoring.endpoint.TimerEndpointResponse;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.springboot.service.test.TestApplication;
import io.github.resilience4j.springboot.service.test.micrometer.FixedOnFailureTagResolver;
import io.github.resilience4j.springboot.service.test.micrometer.QualifiedClassNameOnFailureTagResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = TestApplication.class)
@AutoConfigureTestRestTemplate
public class TimerAutoConfigurationTest {

    @Autowired
    private TestRestTemplate httpClient;
    @Autowired
    private TimerRegistry registry;

    @Test
    public void shouldConfigureTimersUsingConfigurationProperties() {
        Timer timer = registry.timer("backend");
        assertThat(timer).isNotNull();
        assertThat(timer.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.calls");
        assertThat(timer.getTimerConfig().getOnFailureTagResolver().apply(new RuntimeException())).isEqualTo("RuntimeException");

        Timer timerA = registry.timer("backendA");
        assertThat(timerA).isNotNull();
        assertThat(timerA.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.callsA");
        assertThat(timer.getTimerConfig().getOnFailureTagResolver().apply(new RuntimeException())).isEqualTo("RuntimeException");

        Timer timerB = registry.timer("backendB");
        assertThat(timerB).isNotNull();
        assertThat(timerB.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.calls");
        assertThat(timerB.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(QualifiedClassNameOnFailureTagResolver.class);

        Timer timerC = registry.timer("backendC");
        assertThat(timerC).isNotNull();
        assertThat(timerC.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.callsC");
        assertThat(timerC.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(QualifiedClassNameOnFailureTagResolver.class);

        Timer timerD = registry.timer("backendD");
        assertThat(timerD).isNotNull();
        assertThat(timerD.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.callsShared");
        assertThat(timerD.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(FixedOnFailureTagResolver.class);

        Timer timerE = registry.timer("backendE");
        assertThat(timerE).isNotNull();
        assertThat(timerE.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.callsE");
        assertThat(timerE.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(FixedOnFailureTagResolver.class);

        Timer timerF = registry.timer("backendF");
        assertThat(timerF).isNotNull();
        assertThat(timerF.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.callsShared");
        assertThat(timerF.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(QualifiedClassNameOnFailureTagResolver.class);

        Timer timerG = registry.timer("backendG");
        assertThat(timerG).isNotNull();
        assertThat(timerG.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.callsG");
        assertThat(timerG.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(QualifiedClassNameOnFailureTagResolver.class);

        Timer timerH = registry.timer("backendH");
        assertThat(timerH).isNotNull();
        assertThat(timerH.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.callsHCustomized");
        assertThat(timerH.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(FixedOnFailureTagResolver.class);

        Timer timerI = registry.timer("backendI");
        assertThat(timerI).isNotNull();
        assertThat(timerI.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.callsICustomized");
        assertThat(timerI.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(FixedOnFailureTagResolver.class);

        List<String> timers = httpClient.getForEntity("/actuator/timers", TimerEndpointResponse.class).getBody().getTimers();
        then(timers).isEqualTo(List.of(
                timer.getName(), timerA.getName(), timerB.getName(), timerC.getName(), timerD.getName(), timerE.getName(), timerF.getName(), timerG.getName(), timerH.getName(), timerI.getName()
        ));
    }
}
