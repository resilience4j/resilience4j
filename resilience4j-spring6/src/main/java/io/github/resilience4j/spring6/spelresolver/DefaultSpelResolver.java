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
package io.github.resilience4j.spring6.spelresolver;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class DefaultSpelResolver implements EmbeddedValueResolverAware, SpelResolver {
    private static final Pattern PLACEHOLDER_SPEL_REGEX = Pattern.compile("^\\$\\{.+}$");
    private static final Pattern SPEL_TEMPLATE_REGEX = Pattern.compile("^#\\{.+}$");
    private static final Pattern METHOD_SPEL_REGEX = Pattern.compile("^#.+$");
    private static final Pattern BEAN_SPEL_REGEX = Pattern.compile("^@.+");

    private final SpelExpressionParser expressionParser;
    private final ParameterNameDiscoverer parameterNameDiscoverer;
    private final BeanFactory beanFactory;
    private StringValueResolver stringValueResolver;

    public DefaultSpelResolver(SpelExpressionParser spelExpressionParser, ParameterNameDiscoverer parameterNameDiscoverer, BeanFactory beanFactory) {
        this.expressionParser = spelExpressionParser;
        this.parameterNameDiscoverer = parameterNameDiscoverer;
        this.beanFactory = beanFactory;
    }

    @Override
    public String resolve(Method method, Object[] arguments, String spelExpression) {
        if (!StringUtils.hasText(spelExpression)) {
            return spelExpression;
        }

        if (PLACEHOLDER_SPEL_REGEX.matcher(spelExpression).matches() && stringValueResolver != null) {
            return stringValueResolver.resolveStringValue(spelExpression);
        }

        if (SPEL_TEMPLATE_REGEX.matcher(spelExpression).matches()) {
            SpelRootObject rootObject = new SpelRootObject(method, arguments);
            MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(rootObject, method, arguments, parameterNameDiscoverer);
            evaluationContext.setBeanResolver(new BeanFactoryResolver(this.beanFactory));
            Object evaluated = expressionParser.parseExpression(spelExpression, new TemplateParserContext()).getValue(evaluationContext);
            return (String) evaluated;
        }

        if (METHOD_SPEL_REGEX.matcher(spelExpression).matches()) {
            SpelRootObject rootObject = new SpelRootObject(method, arguments);
            MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(rootObject, method, arguments, parameterNameDiscoverer);
            Object evaluated = expressionParser.parseExpression(spelExpression).getValue(evaluationContext);

            return (String) evaluated;
        }

        if (BEAN_SPEL_REGEX.matcher(spelExpression).matches()) {
            SpelRootObject rootObject = new SpelRootObject(method, arguments);

            MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(rootObject, method, arguments, parameterNameDiscoverer);
            evaluationContext.setBeanResolver(new BeanFactoryResolver(this.beanFactory));

            Object evaluated = expressionParser.parseExpression(spelExpression).getValue(evaluationContext);

            return (String) evaluated;
        }

        return spelExpression;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.stringValueResolver = resolver;
    }
}
