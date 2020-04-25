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

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;

public class SpelResolver implements EmbeddedValueResolverAware {
    private static final String BEAN_SPEL_REGEX = "^[$#]\\{.+}$";
    private static final String METHOD_SPEL_REGEX = "^#.+$";

    private final SpelExpressionParser expressionParser;
    private final ParameterNameDiscoverer parameterNameDiscoverer;
    private StringValueResolver stringValueResolver;

    public SpelResolver(SpelExpressionParser spelExpressionParser, ParameterNameDiscoverer parameterNameDiscoverer) {
        this.expressionParser = spelExpressionParser;
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    public String resolve(Method method, Object[] arguments, String spelExpression) {
        if (StringUtils.isEmpty(spelExpression)) {
            return spelExpression;
        }

        if (spelExpression.matches(BEAN_SPEL_REGEX) && stringValueResolver != null) {
            return stringValueResolver.resolveStringValue(spelExpression);
        }

        if (spelExpression.matches(METHOD_SPEL_REGEX)) {
            SpelRootObject rootObject = new SpelRootObject(method, arguments);
            MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(rootObject, method, arguments, parameterNameDiscoverer);
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