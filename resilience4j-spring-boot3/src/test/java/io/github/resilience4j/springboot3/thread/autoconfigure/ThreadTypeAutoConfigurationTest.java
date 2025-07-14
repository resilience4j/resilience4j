/*
 * Copyright 2025 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.springboot3.thread.autoconfigure;

import io.github.resilience4j.core.ExecutorServiceFactory;
import io.github.resilience4j.core.ThreadType;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = TestThreadTypeApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestPropertySource(properties = {
    "resilience4j.thread.type=virtual"
})
public class ThreadTypeAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @After
    public void tearDown() {
        // Reset the system property to not affect other tests
        System.clearProperty("resilience4j.thread.type");
    }

    @Test
    public void testThreadTypePropertyIsSetToSystemProperty() {
        try {
            // Reset the property to ensure clean test state
            System.clearProperty("resilience4j.thread.type");
            
            // Force Spring to set the property again (calling constructor manually)
            new Resilience4jThreadAutoConfiguration(context.getBean(ThreadTypeProperties.class));
            
            // Verify that auto-configuration has propagated the property
            ThreadTypeProperties properties = context.getBean(ThreadTypeProperties.class);
            
            // Check that the property value is correctly read from application properties
            assertThat(properties.getType()).isEqualTo(ThreadType.VIRTUAL);
            
            // Verify that the system property was set correctly by the auto-configuration
            String systemProperty = System.getProperty("resilience4j.thread.type");
            assertThat(systemProperty).isEqualTo(ThreadType.VIRTUAL.toString());
            
            // Verify that ExecutorServiceFactory correctly recognizes the virtual thread configuration
            ThreadType threadType = ExecutorServiceFactory.getThreadType();
            assertThat(threadType).isEqualTo(ThreadType.VIRTUAL);
        } finally {
            // Always clean up to ensure no side effects
            System.clearProperty("resilience4j.thread.type");
        }
    }

    @Test
    public void testThreadTypeAutoConfigurationRegistered() {
        try {
            // Verify that the auto-configuration class is registered in the context
            assertThat(context.getBean(Resilience4jThreadAutoConfiguration.class)).isNotNull();
            
            // Check that the bean is working correctly by manually triggering the property setting
            System.clearProperty("resilience4j.thread.type");
            
            // The auto-configuration should set the system property when its constructor is called
            new Resilience4jThreadAutoConfiguration(context.getBean(ThreadTypeProperties.class));
            
            // System property should now be set
            assertThat(System.getProperty("resilience4j.thread.type")).isEqualTo(ThreadType.VIRTUAL.toString());
        } finally {
            // Clean up after test
            System.clearProperty("resilience4j.thread.type");
        }
    }
}