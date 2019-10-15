package io.github.resilience4j.prometheus;

import io.prometheus.client.CollectorRegistry;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class CallMeterTest {

    @Test
    public void testInstrumentsSuccessfulCall() throws Exception {
        final CollectorRegistry registry = new CollectorRegistry();
        final CallMeter timer = CallMeter.ofCollectorRegistry("some_call", "Some help", registry);

        timer.executeRunnable(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail();
            }
        });

        assertThat(registry.getSampleValue(
                "some_call_total",
                new String[]{},
                new String[]{}))
            .isEqualTo(1.0);
        assertThat(registry.getSampleValue(
                "some_call_failures_total",
                new String[]{},
                new String[]{}))
                .isEqualTo(0.0);
        assertThat(registry.getSampleValue(
                "some_call_latency_count",
                new String[]{},
                new String[]{}))
                .isEqualTo(1.0);
    }

    @Test
    public void testInstrumentsFailedCall() throws Exception {
        final CollectorRegistry registry = new CollectorRegistry();
        final CallMeter timer = CallMeter.ofCollectorRegistry("some_call", "Some help", registry);

        try {
            timer.executeRunnable(() -> {
                try {
                    Thread.sleep(50);
                    throw new SomeAppException("Test Exception");
                } catch (InterruptedException e) {
                    fail();
                }
            });
        } catch (SomeAppException e){
            assertThat(e.getMessage()).isEqualTo("Test Exception");
        }

        assertThat(registry.getSampleValue(
                "some_call_total",
                new String[]{},
                new String[]{}))
                .isEqualTo(1.0);
        assertThat(registry.getSampleValue(
                "some_call_failures_total",
                new String[]{},
                new String[]{}))
                .isEqualTo(1.0);
        assertThat(registry.getSampleValue(
                "some_call_latency_count",
                new String[]{},
                new String[]{}))
                .isEqualTo(0.0);
    }

    @Test
    public void testInstrumentsSuccessfulCallWithLabels() throws Exception {
        final CollectorRegistry registry = new CollectorRegistry();
        final CallMeter timer = CallMeter
                .builder()
                .name("some_call")
                .help("Some call help")
                .labelNames("label_1")
                .build()
                .register(registry);

        timer.labels("boo").executeRunnable(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail();
            }
        });

        assertThat(registry.getSampleValue(
                "some_call_total",
                new String[]{ "label_1" },
                new String[]{ "boo" }))
                .isEqualTo(1.0);
        assertThat(registry.getSampleValue(
                "some_call_failures_total",
                new String[]{ "label_1" },
                new String[]{ "boo" }))
                .isEqualTo(0.0);
        assertThat(registry.getSampleValue(
                "some_call_latency_count",
                new String[]{ "label_1" },
                new String[]{ "boo" }))
                .isEqualTo(1.0);
    }

    @Test
    public void testInstrumentsFailedCallWithLabels() throws Exception {
        final CollectorRegistry registry = new CollectorRegistry();
        final CallMeter timer = CallMeter
                .builder()
                .name("some_call")
                .help("Some test help")
                .labelNames("label_1")
                .build()
                .register(registry);

        try {
            timer.labels("foo").executeRunnable(() -> {
                try {
                    Thread.sleep(50);
                    throw new SomeAppException("Test Exception");
                } catch (InterruptedException e) {
                    fail();
                }
            });
        } catch (SomeAppException e){
            assertThat(e.getMessage()).isEqualTo("Test Exception");
        }

        assertThat(registry.getSampleValue(
                "some_call_total",
                new String[]{ "label_1" },
                new String[]{ "foo" }))
                .isEqualTo(1.0);
        assertThat(registry.getSampleValue(
                "some_call_failures_total",
                new String[]{ "label_1" },
                new String[]{ "foo" }))
                .isEqualTo(1.0);
        assertThat(registry.getSampleValue(
                "some_call_latency_count",
                new String[]{ "label_1" },
                new String[]{ "foo" }))
                .isEqualTo(0.0);
    }

    private static class SomeAppException extends RuntimeException {
        SomeAppException(String message) {
            super(message);
        }
    }
}
