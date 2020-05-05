package io.github.resilience4j.configuration.bulkhead;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

public class BulkheadConfigurationTest {
    private static final BulkheadConfig DEFAULT_CONFIG = BulkheadConfig.ofDefaults();

    private Configuration aConfiguration;

    @Before
    public void initializePropertiesConfiguration() throws ConfigurationException {
        aConfiguration = spy(new Configurations().properties(this.getClass().getClassLoader().getResource("bulkhead-config.properties")));
    }

    @Test
    public void testGetDefaultAllConfig() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration).getDefault();

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(1);
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetNamedAllConfig() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration).get("named");

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(Duration.ofSeconds(2));
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetCustomContextDefaultAllConfig() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration, "custom.prop.bulkhead").getDefault();

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(Duration.ofSeconds(3));
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetCustomContextNamedAllConfig() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration, "custom.prop.bulkhead").get("named");

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(4);
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(Duration.ofSeconds(4));
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetNamedMaxConcurrentOverride() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration).get("concurrent");

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(5);
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetNamedMaxWaitTimeOverride() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration).get("wait");

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(1);
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(Duration.ofSeconds(6));
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetNamedStackTraceOverride() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration).get("stack");

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(1);
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isFalse();
    }

    @Test
    public void testGetNamedNoDefaultConfigMaxConcurrentOverride() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration, "custom.nodefault.bulkhead").get("concurrent");

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(7);
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(DEFAULT_CONFIG.getMaxWaitDuration());
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetNamedNoDefaultConfigMaxWaitOverride() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration, "custom.nodefault.bulkhead").get("wait");

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(DEFAULT_CONFIG.getMaxConcurrentCalls());
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(Duration.ofSeconds(8));
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }

    @Test
    public void testGetNamedNoDefaultStackTraceOverride() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration, "custom.nodefault.bulkhead").get("stack");

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(DEFAULT_CONFIG.getMaxConcurrentCalls());
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(DEFAULT_CONFIG.getMaxWaitDuration());
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isFalse();
    }

    @Test
    public void testGetNamedDoesNotExist() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration).get("nonexistent");

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(1);
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(Duration.ofSeconds(1));
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isTrue();
    }

    @Test
    public void testGetDefaultsContextDoesNotExist() {
        given(aConfiguration.getInt(anyString(), anyInt())).willCallRealMethod();
        given(aConfiguration.getLong(anyString(), anyLong())).willCallRealMethod();
        given(aConfiguration.getBoolean(anyString(), anyBoolean())).willCallRealMethod();

        BulkheadConfig bulkheadConfig = new BulkheadConfiguration(aConfiguration, "context.does.not.exist").getDefault();

        assertThat(bulkheadConfig.getMaxConcurrentCalls()).isEqualTo(DEFAULT_CONFIG.getMaxConcurrentCalls());
        assertThat(bulkheadConfig.getMaxWaitDuration()).isEqualByComparingTo(DEFAULT_CONFIG.getMaxWaitDuration());
        assertThat(bulkheadConfig.isWritableStackTraceEnabled()).isEqualTo(DEFAULT_CONFIG.isWritableStackTraceEnabled());
    }
}
