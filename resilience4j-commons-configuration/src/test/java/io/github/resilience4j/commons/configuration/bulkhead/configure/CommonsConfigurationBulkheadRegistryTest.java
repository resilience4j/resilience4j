package io.github.resilience4j.commons.configuration.bulkhead.configure;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.commons.configuration.util.Constants;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class CommonsConfigurationBulkheadRegistryTest {

    @Test
    public void testBulkheadRegistryFromCommonsConfiguration() throws ConfigurationException {
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                .configure(new Parameters()
                        .fileBased()
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(Constants.LIST_DELIMITER))
                        .setFileName(Constants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME));
        Configuration config = builder.getConfiguration();

        BulkheadRegistry registry = CommonsConfigurationBulkheadRegistry.of(config, new CompositeCustomizer<>(List.of()));

        Assertions.assertThat(registry.bulkhead(Constants.BACKEND_A).getName()).isEqualTo(Constants.BACKEND_A);
        Assertions.assertThat(registry.bulkhead(Constants.BACKEND_B).getName()).isEqualTo(Constants.BACKEND_B);
    }
}