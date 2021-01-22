package io.github.resilience4j.bulkhead.adaptive.internal.amid;


import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitDecreasedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitIncreasedEvent;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.*;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MockServerContainer;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Ignore("uses Docker")
public class AdaptiveBulkheadLimitsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveBulkheadLimitsTest.class);
    private static MockServerContainer mockServerContainer;
    private static MockServerClient client;

    static {
        mockServerContainer = new MockServerContainer();
        mockServerContainer.start();
        client = new MockServerClient(mockServerContainer.getContainerIpAddress(),
            mockServerContainer.getServerPort());
    }

    // enable if u need to see the graphs of the executions
    private boolean drawGraphs = false;
    private List<LogEntry> maxConcurrentCalls = new CopyOnWriteArrayList<>();
    private AdaptiveBulkhead bulkhead;
    private AdaptiveBulkheadConfig config;
    private final String baseUrl = mockServerContainer.getEndpoint() + "/testService/2";

    @Before
    public void init() {
        maxConcurrentCalls.clear();
        config = AdaptiveBulkheadConfig.builder()
                .maxConcurrentRequestsLimit(50)
                .minConcurrentRequestsLimit(10)
                .slidingWindowSize(20)
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(150)
                .build();

        bulkhead = AdaptiveBulkhead.of("test", config);
        bulkhead.getEventPublisher().onEvent(event -> {
            LOGGER.info("Received event {}: {}  ", event.getEventType(),
                event.eventData().entrySet().toString());
            if (event instanceof BulkheadOnLimitDecreasedEvent
                || event instanceof BulkheadOnLimitIncreasedEvent) {
                maxConcurrentCalls
                    .add(new LogEntry(event.getCreationTime().getNano(),
                        Double.parseDouble(event.eventData().get("newMaxConcurrentCalls"))));

            }
        });
        final Duration duration = Duration.ofMillis(randomLatency(1, 200));
        client.when(HttpRequest.request()
            .withPath("/testService/2"))
            .respond(HttpResponse.response().withStatusCode(200)
                .withBody("{\"msgCode\":\"2\",\"msg\":\"2000000\"}")
                .withDelay(TimeUnit.MILLISECONDS, duration.toMillis())
                .withHeader("Content-Type", "application/json"));
    }

    @Test
    public void testProtectedServiceBehindTheGateway() {
        assertThat(config).isNotNull();
        assertThat(bulkhead).isNotNull();
        ExecutorService executorService = Executors
            .newFixedThreadPool(20);
        List<Callable<HttpResponse>> parallelCalls = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            parallelCalls.add(this::callProtectedApi);
        }
        try {
            executorService.invokeAll(parallelCalls);
        } catch (InterruptedException e) {
            LOGGER.error(
                "InterruptedException  has been thrown while waiting for all parallel tasks to be done",
                e);
        }
        LOGGER
            .info("finished the TEST with reported matrices size : {}", maxConcurrentCalls.size());
        executorService.shutdown();
    }

    public HttpResponse callProtectedApi() {
        // increase the number of loop count for bigger load and better call distribution , set to 100 only for limiting testing execution time
        for (int i = 0; i < 100; i++) {
            try {
                bulkhead.executeCheckedSupplier(() -> {
                    LOGGER.info("Calling endpoint : " + baseUrl);
                    org.apache.http.HttpResponse response = hitTheServerWithPostRequest(
                        baseUrl);
                    try {
                        LOGGER
                            .info("Received: status->{}, payload->{}",
                                response.getStatusLine().getStatusCode(),
                                EntityUtils.toString(response.getEntity(), "UTF-8"));
                    } catch (IOException e) {
                        LOGGER.error("Parsing response entity failed ", e);
                    }
                    return response;
                });
            } catch (Throwable throwable) {
                LOGGER.error("error from  adaptive bulkhead", throwable);
            }
        }
        return null;
    }


    private org.apache.http.HttpResponse hitTheServerWithPostRequest(String getUrl) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(getUrl);
        httpGet.setHeader("Content-type", "application/json");
        try {
            return client.execute(httpGet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @AfterClass
    public static void close() {
        mockServerContainer.close();
    }

    @After
    public void beforeFinish() {
        if (drawGraphs) {
            drawChart();
        }
    }

    private void drawChart() {
        // Create Chart
        if (!maxConcurrentCalls.isEmpty()) {
            Collections.sort(maxConcurrentCalls);
            XYChart chart2 = new XYChartBuilder().width(800).height(600)
                .title("Adaptive bulkhead concurrency")
                .xAxisTitle("Seconds").yAxisTitle("Max Concurrent Limit").build();
            chart2.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
            chart2.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
            chart2.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
            chart2.getStyler().setYAxisDecimalPattern("Max Concurrent Limit #");
            chart2.getStyler().setPlotMargin(0);
            chart2.getStyler().setPlotContentSize(0.50);
            chart2.addSeries("MaxConcurrentCalls", maxConcurrentCalls.stream().map(
                LogEntry::getTime).collect(
                Collectors.toList()), maxConcurrentCalls.stream().map(
                LogEntry::getConcurrentCalls).collect(
                Collectors.toList()));
            try {
                BitmapEncoder.saveJPGWithQuality(chart2, "./AdaptiveBulkheadConcurrency.jpg", 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private long randomLatency(int min, int max) {
        return min + ThreadLocalRandom.current().nextLong(max - min);
    }
}
