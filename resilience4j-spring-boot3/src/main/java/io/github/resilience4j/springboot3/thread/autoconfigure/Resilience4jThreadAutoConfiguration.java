package io.github.resilience4j.springboot3.thread.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration that propagates the {@code resilience4j.thread.type} property defined in
 * {@code application.yml} (or other Spring property sources) to the JVM system properties so that
 * {@link io.github.resilience4j.core.ExecutorServiceFactory} can pick it up at startup.
 *
 * <p>Supported values: {@code virtual} or {@code platform} (default).</p>
 *
 * <p>This configuration is activated iff the property {@code resilience4j.thread.type} is present.
 * If the system property is already set (e.g. via JVM arg) it is left untouched so that
 * command-line options win over Spring configuration.</p>
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ThreadTypeProperties.class)
@ConditionalOnProperty(prefix = "resilience4j.thread", name = "type")
public class Resilience4jThreadAutoConfiguration {

    public Resilience4jThreadAutoConfiguration(ThreadTypeProperties properties) {
        // Transfer to system property only if not already specified
        if (System.getProperty("resilience4j.thread.type") == null) {
            System.setProperty("resilience4j.thread.type", properties.getType().toString());
        }
    }
}
