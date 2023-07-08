package io.github.resilience4j.commons.configuration.retry.configure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.commons.configuration.util.CommonsConfigurationUtil;
import io.github.resilience4j.commons.configuration.util.TestConstants;
import io.github.resilience4j.retry.RetryRegistry;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class CommonsConfigurationRetryRegistryTest {

    @Test
    public void testRetryRegistryFromPropertiesFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        RetryRegistry registry = CommonsConfigurationRetryRegistry.of(config, new CompositeCustomizer<>(List.of()));

        Assertions.assertThat(registry.retry(TestConstants.BACKEND_A).getName()).isEqualTo(TestConstants.BACKEND_A);
        Assertions.assertThat(registry.retry(TestConstants.BACKEND_B).getName()).isEqualTo(TestConstants.BACKEND_B);
    }

    @Test
    public void testRetryRegistryFromYamlFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        RetryRegistry registry = CommonsConfigurationRetryRegistry.of(config, new CompositeCustomizer<>(List.of()));

        Assertions.assertThat(registry.retry(TestConstants.BACKEND_A).getName()).isEqualTo(TestConstants.BACKEND_A);
        Assertions.assertThat(registry.retry(TestConstants.BACKEND_B).getName()).isEqualTo(TestConstants.BACKEND_B);
    }
}