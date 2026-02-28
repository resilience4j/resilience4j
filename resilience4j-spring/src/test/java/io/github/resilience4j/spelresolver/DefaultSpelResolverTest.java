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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.github.resilience4j.DummySpelBean;
import io.github.resilience4j.TestApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestApplication.class, properties = "property=backend")
public class DefaultSpelResolverTest {
    private DefaultSpelResolver sut;

    @Autowired
    private ConfigurableBeanFactory configurableBeanFactory;

    @MockBean(name="dummySpelBean")
    DummySpelBean dummySpelBean;

    @Before
    public void setUp() {
        sut = new DefaultSpelResolver(new SpelExpressionParser(), new StandardReflectionParameterNameDiscoverer(), configurableBeanFactory);
        sut.setEmbeddedValueResolver(new EmbeddedValueResolver(configurableBeanFactory));
    }

    @Test
    public void givenNonSpelExpression_whenParse_returnsItself() throws Exception {
        String testExpression = "backendA";

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
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

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
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

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, testExpression);

        assertThat(result).isEqualTo("testMethod");
    }

    /**
     * #root.className
     */
    @Test
    public void testRootClassName() throws Exception {
        String testExpression = "#root.className";

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, testExpression);

        assertThat(result).isEqualTo("io.github.resilience4j.spelresolver.DefaultSpelResolverTest");
    }

    /**
     * #p0
     */
    @Test
    public void testP0() throws Exception {
        String testExpression = "#p0";
        String firstArgument = "test";

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
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

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
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

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
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

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
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

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, testExpression);

        assertThat(result).isEqualTo("backend");
    }

    @Test
    public void beanMethodSpelTest() throws Exception {
        String testExpression = "@dummySpelBean.getBulkheadName(#parameter)";
        String testMethodArg = "argg";
        String bulkheadName = "sgt. bulko";
        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        given(dummySpelBean.getBulkheadName(testMethodArg)).willReturn(bulkheadName);

        String result = sut.resolve(testMethod, new Object[]{testMethodArg}, testExpression);

        then(dummySpelBean).should(times(1)).getBulkheadName(testMethodArg);
        assertThat(result).isEqualTo(bulkheadName);
    }

    @Test
    public void atTest() throws Exception {
        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, "@");

        assertThat(result).isEqualTo("@");
    }

    @Test
    public void nullTest() throws Exception {
        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, null);

        assertThat(result).isNull();
    }

    @Test
    public void emptyStringTest() throws Exception {
        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, "");

        assertThat(result).isEqualTo("");
    }

    @Test
    public void dollarTest() throws Exception {
        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{}, "$");

        assertThat(result).isEqualTo("$");
    }

    /**
     * #{'one.' + #root.args[0]} - SpEL template with method args context
     */
    @Test
    public void spelTemplateWithArgsTest() throws Exception {
        String testExpression = "#{'one.' + #root.args[0]}";
        String firstArgument = "two";

        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        String result = sut.resolve(testMethod, new Object[]{firstArgument}, testExpression);

        assertThat(result).isEqualTo("one.two");
    }

    /**
     * #{'prefix.' + @dummySpelBean.getBulkheadName(#parameter)} - SpEL template with bean reference
     */
    @Test
    public void spelTemplateWithBeanReferenceTest() throws Exception {
        String testExpression = "#{'prefix.' + @dummySpelBean.getBulkheadName(#parameter)}";
        String testMethodArg = "argg";
        String bulkheadName = "sgt. bulko";
        DefaultSpelResolverTest target = new DefaultSpelResolverTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        given(dummySpelBean.getBulkheadName(testMethodArg)).willReturn(bulkheadName);

        String result = sut.resolve(testMethod, new Object[]{testMethodArg}, testExpression);

        then(dummySpelBean).should(times(1)).getBulkheadName(testMethodArg);
        assertThat(result).isEqualTo("prefix.sgt. bulko");
    }

    public String testMethod(String parameter) {
        return "test";
    }
}