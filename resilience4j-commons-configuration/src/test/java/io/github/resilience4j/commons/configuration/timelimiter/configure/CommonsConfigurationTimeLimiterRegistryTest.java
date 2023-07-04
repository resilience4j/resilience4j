package io.github.resilience4j.commons.configuration.timelimiter.configure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.commons.configuration.util.TestConstants;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class CommonsConfigurationTimeLimiterRegistryTest {
    @Test
    public void testRateLimiterRegistryFromCommonsConfiguration() throws ConfigurationException {
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                .configure(new Parameters()
                        .fileBased()
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(TestConstants.LIST_DELIMITER))
                        .setFileName(TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME));
        Configuration config = builder.getConfiguration();

        TimeLimiterRegistry registry = CommonsConfigurationTimeLimiterRegistry.of(config, new CompositeCustomizer<>(List.of()));

        Assertions.assertThat(registry.timeLimiter(TestConstants.BACKEND_A).getName()).isEqualTo(TestConstants.BACKEND_A);
        Assertions.assertThat(registry.timeLimiter(TestConstants.BACKEND_B).getName()).isEqualTo(TestConstants.BACKEND_B);
    }
}