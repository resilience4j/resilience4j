/*
 *   Copyright 2023: Deepak Kumar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

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
