package io.github.resilience4j.commons.configuration.util;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class CommonsConfigurationUtil {
    public static <T extends FileBasedConfiguration> Configuration getConfiguration(final Class<T> configBuilderType,
                                                                                     final String configFileName) throws ConfigurationException {
        FileBasedConfigurationBuilder<T> builder = new FileBasedConfigurationBuilder<>(configBuilderType)
                .configure(new Parameters()
                        .fileBased()
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(TestConstants.LIST_DELIMITER))
                        .setFileName(configFileName));
        return builder.getConfiguration();
    }
}
