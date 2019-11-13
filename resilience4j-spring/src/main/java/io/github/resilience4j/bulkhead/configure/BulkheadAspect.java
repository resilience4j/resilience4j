/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a
 * {@link Bulkhead} annotation. The aspect will handle methods that return a
 * RxJava2 reactive type, Spring Reactor reactive type, CompletionStage type, or
 * value type.
 * <p>
 * The BulkheadRegistry is used to retrieve an instance of a Bulkhead for a
 * specific name.
 * <p>
 * Given a method like this:
 * <pre><code>
 *     {@literal @}Bulkhead(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre> each time the {@code #fancyName(String)} method is invoked, the
 * method's execution will pass through a a
 * {@link io.github.resilience4j.bulkhead.Bulkhead} according to the given
 * config.
 * <p>
 * The fallbackMethod parameter signature must match either:
 * <p>
 * 1) The method parameter signature on the annotated method or 2) The method
 * parameter signature with a matching exception type as the last parameter on
 * the annotated method
 */
@Aspect
public class BulkheadAspect implements Ordered {

    private final BulkheadAspectHelper bulkheadAspectHelper;
    private final BulkheadConfigurationProperties bulkheadConfigurationProperties;

    public BulkheadAspect(BulkheadAspectHelper bulkheadAspectHelper, BulkheadConfigurationProperties backendMonitorPropertiesRegistry) {
        this.bulkheadAspectHelper = bulkheadAspectHelper;
        this.bulkheadConfigurationProperties = backendMonitorPropertiesRegistry;
    }

    @Pointcut(value = "@within(Bulkhead) || @annotation(Bulkhead)", argNames = "Bulkhead")
    public void matchAnnotatedClassOrMethod(Bulkhead Bulkhead) {
    }

    @Around(value = "matchAnnotatedClassOrMethod(bulkheadAnnotation)", argNames = "proceedingJoinPoint, bulkheadAnnotation")
    public Object bulkheadAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable Bulkhead bulkheadAnnotation) throws Throwable {
        ProceedingJoinPointHelper joinPointHelper = ProceedingJoinPointHelper.prepareFor(proceedingJoinPoint);
        if (bulkheadAnnotation == null) {
            bulkheadAnnotation = joinPointHelper.getClassAnnotation(Bulkhead.class);
        }
        if (bulkheadAnnotation == null) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        bulkheadAspectHelper.decorate(joinPointHelper, bulkheadAnnotation);
        return joinPointHelper.getDecoratedProceedCall().apply();
    }

    @Override
    public int getOrder() {
        return bulkheadConfigurationProperties.getBulkheadAspectOrder();
    }
}
