/*
 * Copyright 2020 Kyuhyen Hwang
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
package io.github.resilience4j.spelresolver;

import io.github.resilience4j.TestApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class, properties = "property=backend")
public class SpelResolverTest {
    private SpelResolver sut;

    @Autowired
    private ConfigurableBeanFactory configurableBeanFactory;

    @Before
    public void setUp() {
        sut = new SpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer());
        sut.setEmbeddedValueResolver(new EmbeddedValueResolver(configurableBeanFactory));
    }

    @Test
    public void givenNonSpelExpression_whenParse_returnsItself() throws Exception {
        String testExpression = "backendA";

        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, testExpression);

        assertThat(result).isEqualTo(testExpression);
    }

    /**
     * #root.args[0]
     */
    @Test
    public void testRootArgs() throws Exception {
        String testExpression = "#root.args[0]";
        String firstArgument = "test";

        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{firstArgument}, testExpression);

        assertThat(result).isEqualTo(firstArgument);
    }

    /**
     * #root.methodName
     */
    @Test
    public void testRootMethodName() throws Exception {
        String testExpression = "#root.methodName";

        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, testExpression);

        assertThat(result).isEqualTo("testMethod");
    }

    /**
     * #p0
     */
    @Test
    public void testP0() throws Exception {
        String testExpression = "#p0";
        String firstArgument = "test";

        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{firstArgument}, testExpression);

        assertThat(result).isEqualTo(firstArgument);
    }

    /**
     * #a0
     */
    @Test
    public void testA0() throws Exception {
        String testExpression = "#a0";
        String firstArgument = "test";

        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{firstArgument}, testExpression);

        assertThat(result).isEqualTo(firstArgument);
    }

    /**
     * #{'recover'}
     */
    @Test
    public void stringSpelTest() throws Exception {
        String testExpression = "#{'recover'}";

        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, testExpression);

        assertThat(result).isEqualTo("recover");
    }

    /**
     * ${missingProperty:default}
     */
    @Test
    public void placeholderSpelTest() throws Exception {
        String testExpression = "${missingProperty:default}";

        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, testExpression);

        assertThat(result).isEqualTo("default");
    }

    /**
     * ${property:default}
     */
    @Test
    public void placeholderSpelTest2() throws Exception {
        String testExpression = "${property:default}";

        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, testExpression);

        assertThat(result).isEqualTo("backend");
    }

    @Test
    public void nullTest() throws Exception {
        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, null);

        assertThat(result).isNull();
    }

    @Test
    public void emptyStringTest() throws Exception {
        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, "");

        assertThat(result).isEqualTo("");
    }

    @Test
    public void dollarTest() throws Exception {
        SpelResolverTest target = new SpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, "$");

        assertThat(result).isEqualTo("$");
    }

    public String testMethod(String parameter) {
        return "test";
    }
}