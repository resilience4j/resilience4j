package io.github.resilience4j.service.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

/**
 * @author krnsaurabh
 */
@SpringBootApplication(
		scanBasePackageClasses = {
				TestApplicationWithoutMetricsAutoConfiguration.class
		},
		exclude = {
				ErrorMvcAutoConfiguration.class,
				MetricsAutoConfiguration.class
		}
)
public class TestApplicationWithoutMetricsAutoConfiguration {
	public static void main(String[] args) {
		SpringApplication.run(TestApplicationWithoutMetricsAutoConfiguration.class, args);
	}
}
