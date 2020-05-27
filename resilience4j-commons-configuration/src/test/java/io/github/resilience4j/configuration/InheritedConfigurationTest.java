package io.github.resilience4j.configuration;

import org.apache.commons.configuration2.Configuration;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@RunWith(Enclosed.class)
public class InheritedConfigurationTest {

    @RunWith(MockitoJUnitRunner.class)
    public static class UtilityMethodTests {
        private static final String key = "duration";
        private static final Duration defaultDuration = Duration.ofDays(365);

        @Mock
        public Configuration aConfiguration;

        @Mock
        public InheritedConfiguration anInheritedConfiguration;

        @Mock
        public Logger logger;

        @Test
        public void testValidISO8601Duration() {
            given(aConfiguration.getString(key, defaultDuration.toString())).willReturn("PT30S");
            given(anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration)).willCallRealMethod();

            Duration duration = anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration);

            assertThat(duration).isEqualByComparingTo(Duration.ofSeconds(30));
        }

        @Test
        public void testInvalidISO8601Duration() {
            given(aConfiguration.getString(key, defaultDuration.toString())).willReturn("PT3D"); // Invalid
            given(anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration)).willCallRealMethod();

            assertThatThrownBy(() -> {
                anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration);
            }).isInstanceOf(DateTimeParseException.class);
        }

        @Test
        public void testUnconfiguredDuration() {
            given(aConfiguration.getString(key, defaultDuration.toString())).willReturn(defaultDuration.toString());
            given(anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration)).willCallRealMethod();

            Duration duration = anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration);

            assertThat(duration).isEqualByComparingTo(defaultDuration);
        }

        @Test
        public void testDurationAsMillis() {
            given(aConfiguration.getString(key, defaultDuration.toString())).willReturn("3000");
            given(anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration)).willCallRealMethod();

            Duration duration = anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration);

            assertThat(duration).isEqualByComparingTo(Duration.ofMillis(3000));
        }

        @Test
        public void testDurationInvalid() {
            given(aConfiguration.getString(key, defaultDuration.toString())).willReturn("30 SECONDS"); // Invalid
            given(anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration)).willCallRealMethod();

            assertThatThrownBy(() -> {
                anInheritedConfiguration.getDuration(aConfiguration, key, defaultDuration);
            }).isInstanceOf(NumberFormatException.class);
        }

        @Test
        public void testGetThrowableClassesByNameNoDefaults() {
            given(aConfiguration.getStringArray(key)).willReturn(new String[]{"java.lang.RuntimeException", "java.io.IOException"});
            given(anInheritedConfiguration.getLogger()).willReturn(logger);
            given(anInheritedConfiguration.getThrowableClassesByName(aConfiguration, key)).willCallRealMethod();

            Set<Class<?>> throwables = anInheritedConfiguration.getThrowableClassesByName(aConfiguration, key);

            assertThat(throwables).containsOnly(RuntimeException.class, IOException.class);
        }

        @Test
        public void testGetThrowableClassesByNameWithDefaults() {
            given(aConfiguration.getStringArray(key)).willReturn(new String[]{"java.lang.RuntimeException", "java.io.IOException"});
            given(anInheritedConfiguration.getLogger()).willReturn(logger);
            given(anInheritedConfiguration.getThrowableClassesByName(eq(aConfiguration), eq(key), any())).willCallRealMethod();

            Set<Class<?>> throwables = anInheritedConfiguration.getThrowableClassesByName(aConfiguration, key, NullPointerException.class);

            assertThat(throwables).containsOnly(RuntimeException.class, IOException.class);
        }

        @Test
        public void testGetThrowableClassesByNameNoConfiguredValuesNoDefaults() {
            given(aConfiguration.getStringArray(key)).willReturn(new String[]{});
            given(anInheritedConfiguration.getLogger()).willReturn(logger);
            given(anInheritedConfiguration.getThrowableClassesByName(aConfiguration, key)).willCallRealMethod();

            Set<Class<?>> throwables = anInheritedConfiguration.getThrowableClassesByName(aConfiguration, key);

            assertThat(throwables).isNull();
        }

        @Test
        public void testGetThrowableClassesByNameNoConfiguredValuesWithDefaults() {
            given(aConfiguration.getStringArray(key)).willReturn(new String[]{});
            given(anInheritedConfiguration.getLogger()).willReturn(logger);
            given(anInheritedConfiguration.getThrowableClassesByName(eq(aConfiguration), eq(key), any())).willCallRealMethod();

            Set<Class<?>> throwables = anInheritedConfiguration.getThrowableClassesByName(aConfiguration, key, NullPointerException.class);

            assertThat(throwables).containsOnly(NullPointerException.class);
        }

        @Test
        public void testGetThrowableClassesByNameConfiguredValuesNotThrowablesNoDefaults() {
            given(aConfiguration.getStringArray(key)).willReturn(new String[]{this.getClass().getName()});
            given(anInheritedConfiguration.getLogger()).willReturn(logger);
            given(anInheritedConfiguration.getThrowableClassesByName(aConfiguration, key)).willCallRealMethod();

            Set<Class<?>> throwables = anInheritedConfiguration.getThrowableClassesByName(aConfiguration, key);

            assertThat(throwables).isNull();
        }
    }

    @RunWith(MockitoJUnitRunner.class)
    public static class ConfigRetrievalTests {
        private static final String subsetName = "resilience4j";

        private static final Answer<Integer> theDefault = new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(1);
            }
        };

        @Mock
        public Configuration aConfiguration;

        @Mock public Configuration aBaseConfiguration;

        @Mock
        public Configuration aSubsetConfiguration;

        @Mock
        public Configuration aContextConfiguration;

        @Test
        public void testGetConfiguredValue() {
            given(aConfiguration.subset(subsetName)).willReturn(aSubsetConfiguration);
            given(aSubsetConfiguration.subset(anyString())).willReturn(aContextConfiguration);
            given(aContextConfiguration.getInt(any(), anyInt())).willReturn(1);
            TestConfiguration aTestConfiguration = new TestConfiguration(aConfiguration, subsetName);

            TestConfig config = aTestConfiguration.get("key");

            assertThat(config.getValue()).isEqualTo(1);
        }

        @Test
        public void testGetUnconfiguredValueUsesDefault() {
            given(aConfiguration.subset(subsetName)).willReturn(aSubsetConfiguration);
            given(aSubsetConfiguration.subset(anyString())).willReturn(aContextConfiguration);
            given(aContextConfiguration.getInt(any(), anyInt())).willAnswer(theDefault);
            TestConfiguration aTestConfiguration = new TestConfiguration(aConfiguration, subsetName);

            TestConfig config = aTestConfiguration.get("key");

            assertThat(config.getValue()).isEqualTo(aTestConfiguration.getDefaultConfigObject().getValue());
        }

        @Test
        public void testGetConfiguredValueInherited() {
            given(aConfiguration.subset(subsetName)).willReturn(aSubsetConfiguration);
            given(aSubsetConfiguration.subset(anyString())).willReturn(aContextConfiguration);
            given(aContextConfiguration.getString("baseConfig")).willReturn("aBaseConfig");
            given(aContextConfiguration.getInt(anyString(), anyInt())).willAnswer(theDefault);
            given(aSubsetConfiguration.subset("aBaseConfig")).willReturn(aBaseConfiguration);
            given(aBaseConfiguration.getInt(any(), anyInt())).willReturn(1500);
            TestConfiguration aTestConfiguration = new TestConfiguration(aConfiguration, subsetName);

            TestConfig config = aTestConfiguration.get("key");

            assertThat(config.getValue()).isEqualTo(1500);
        }
    }

    private static class TestConfiguration extends InheritedConfiguration<TestConfig> {
        public TestConfiguration(Configuration config, String context) {
            super(config, context);
        }

        @Override
        protected TestConfig map(Configuration config, TestConfig defaults) {
            return new TestConfig(config.getInt("value", defaults.getValue()));
        }

        @Override
        protected TestConfig getDefaultConfigObject() {
            return new TestConfig(Integer.MIN_VALUE);
        }

        @Override
        protected Logger getLogger() {
            return LoggerFactory.getLogger(TestConfiguration.class);
        }
    }

    private static class TestConfig {
        private int value;

        public TestConfig(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
