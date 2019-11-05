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

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;

/**
 * Retry aspect extension support interface type if you want to support new
 * types
 */
public interface RetryAspectExt {
    
    /**
     * @param returnType the AOP method return type class
     * @return boolean true if this extension can handle this type
     */
    boolean canHandleReturnType(Class returnType);

    /**
     * @param joinPointHelper Spring AOP helper which you should decorate using {@link ProceedingJoinPointHelper#decorateProceedCall(java.util.function.Function)}
     * @param retry the configured Retry
     */
    void decorate(ProceedingJoinPointHelper joinPointHelper, Retry retry);
}
