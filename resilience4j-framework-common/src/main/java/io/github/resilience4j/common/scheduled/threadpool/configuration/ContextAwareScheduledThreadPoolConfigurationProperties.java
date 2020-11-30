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

    private int corePoolSize;
    private Class<? extends ContextPropagator>[] contextPropagators = new Class[0];

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 1) {
            throw new IllegalArgumentException(
                "corePoolSize must be a positive integer value >= 1");
        }
        this.corePoolSize = corePoolSize;
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
        List<ContextPropagator> contextPropagatorsList = stream(this.contextPropagators)
            .map(ClassUtils::instantiateClassDefConstructor)
            .collect(toList());

        return ContextAwareScheduledThreadPoolExecutor.newScheduledThreadPool()
            .corePoolSize(this.corePoolSize)
            .contextPropagators(contextPropagatorsList.toArray(new ContextPropagator[0]))
            .build();
    }

}
