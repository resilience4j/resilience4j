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
package io.github.resilience4j.core;

import io.github.resilience4j.core.lang.Nullable;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

public class ContextAwareScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    private final List<ContextPropagator> contextPropagators;
    private static final String THREAD_PREFIX = "ContextAwareScheduledThreadPool";

    private ContextAwareScheduledThreadPoolExecutor(int corePoolSize,
                                                   @Nullable List<ContextPropagator> contextPropagators) {
        super(corePoolSize, new NamingThreadFactory(THREAD_PREFIX));
        this.contextPropagators = contextPropagators != null ? contextPropagators : new ArrayList<>();
    }

    public List<ContextPropagator> getContextPropagators() {
        return Collections.unmodifiableList(this.contextPropagators);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Map<String, String> mdcContextMap = getMdcContextMap();
        return super.schedule(ContextPropagator.decorateRunnable(contextPropagators, () -> {
                try {
                    setMDCContext(mdcContextMap);
                    command.run();
                } finally {
                    MDC.clear();
                }
            }), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        Map<String, String> mdcContextMap = getMdcContextMap();
        return super.schedule(ContextPropagator.decorateCallable(contextPropagators, () -> {
            try {
                setMDCContext(mdcContextMap);
                return callable.call();
            } finally {
                MDC.clear();
            }
        }), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Map<String, String> mdcContextMap = getMdcContextMap();
        return super.scheduleAtFixedRate(ContextPropagator.decorateRunnable(contextPropagators, () -> {
            try {
                setMDCContext(mdcContextMap);
                command.run();
            } finally {
                MDC.clear();
            }
        }), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Map<String, String> mdcContextMap = getMdcContextMap();
        return super.scheduleWithFixedDelay(ContextPropagator.decorateRunnable(contextPropagators, () -> {
            try {
                setMDCContext(mdcContextMap);
                command.run();
            } finally {
                MDC.clear();
            }
        }), initialDelay, delay, unit);
    }

    private Map<String, String> getMdcContextMap() {
        return Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(Collections.emptyMap());
    }

    private void setMDCContext(Map<String, String> contextMap) {
        MDC.clear();
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }

    public static Builder newScheduledThreadPool() {
        return new Builder();
    }

    public static class Builder {
        private List<ContextPropagator> contextPropagators = new ArrayList<>();
        private int corePoolSize;

        public Builder corePoolSize(int corePoolSize) {
            if (corePoolSize < 1) {
                throw new IllegalArgumentException(
                    "corePoolSize must be a positive integer value >= 1");
            }
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder contextPropagators(ContextPropagator... contextPropagators) {
            this.contextPropagators = contextPropagators != null ?
                Arrays.stream(contextPropagators).collect(toList()) :
                new ArrayList<>();
            return this;
        }

        public ContextAwareScheduledThreadPoolExecutor build() {
            return new ContextAwareScheduledThreadPoolExecutor(corePoolSize, contextPropagators);
        }
    }
}
