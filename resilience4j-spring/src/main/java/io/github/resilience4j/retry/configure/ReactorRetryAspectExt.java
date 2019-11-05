/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.retry.configure;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;
import io.vavr.CheckedFunction0;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * the Reactor Retry logic support for the spring AOP Conditional on Reactor
 * class existence on spring class loader
 */
public class ReactorRetryAspectExt implements RetryAspectExt {

    private static final Logger logger = LoggerFactory.getLogger(ReactorRetryAspectExt.class);

    /**
     * @param returnType the AOP method return type class
     * @return boolean if the method has Reactor return type
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean canHandleReturnType(Class returnType) {
        return (Flux.class.isAssignableFrom(returnType)) || (Mono.class.isAssignableFrom(returnType));
    }

    /**
     * handle the Spring web flux (Flux /Mono) return types AOP based into
     * reactor retry See {@link io.github.resilience4j.retry.Retry} for details.
     *
     * @param joinPointHelper Spring AOP helper
     * @param retry the configured retry
     * @return the result object
     */
    @SuppressWarnings("unchecked")
    @Override
    public CheckedFunction0<Object> decorate(ProceedingJoinPointHelper joinPointHelper, io.github.resilience4j.retry.Retry retry) {
        return () -> {
            Object returnValue = joinPointHelper.getDecoratedProceedCall().apply();
            return handleReturnValue(returnValue, retry);
        };
    }

    private Object handleReturnValue(Object returnValue, io.github.resilience4j.retry.Retry retry) {
        if (Flux.class.isAssignableFrom(returnValue.getClass())) {
            Flux<?> fluxReturnValue = (Flux<?>) returnValue;
            return fluxReturnValue.compose(RetryOperator.of(retry));
        } else if (Mono.class.isAssignableFrom(returnValue.getClass())) {
            Mono<?> monoReturnValue = (Mono<?>) returnValue;
            return monoReturnValue.compose(RetryOperator.of(retry));
        } else {
            logger.error("Unsupported type for Reactor retry {}", returnValue.getClass().getTypeName());
            throw new IllegalArgumentException("Not Supported type for the retry in Reactor :" + returnValue.getClass().getName());

        }
    }
}
