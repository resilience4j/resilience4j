/*
 *
 *  Copyright 2020 krnsaurabh
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.common.scheduled.threadpool.configuration;

import io.github.resilience4j.core.ClassUtils;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.ContextPropagator;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class ContextAwareScheduledThreadPoolConfigurationProperties {

    private int coreThreadPoolSize;
    private Class<? extends ContextPropagator>[] contextPropagators = new Class[0];

    public int getCoreThreadPoolSize() {
        return coreThreadPoolSize;
    }

    public void setCoreThreadPoolSize(int coreThreadPoolSize) {
        if (coreThreadPoolSize < 1) {
            throw new IllegalArgumentException(
                "coreThreadPoolSize must be a positive integer value >= 1");
        }
        this.coreThreadPoolSize = coreThreadPoolSize;
    }

    public Class<? extends ContextPropagator>[] getContextPropagators() {
        return contextPropagators;
    }

    public void setContextPropagators(Class<? extends ContextPropagator>... contextPropagators) {
        this.contextPropagators = contextPropagators != null
            ? contextPropagators
            : new Class[0];
    }

    public ContextAwareScheduledThreadPoolExecutor build() {
        if (this.coreThreadPoolSize < 1) {
            return null;
        }
        List<ContextPropagator> contextPropagatorsList = null;
        if (contextPropagators.length > 0) {
            contextPropagatorsList = stream(this.contextPropagators)
                .map(ClassUtils::instantiateClassDefConstructor)
                .collect(toList());
        }
        return new ContextAwareScheduledThreadPoolExecutor(this.coreThreadPoolSize, contextPropagatorsList);
    }

}
