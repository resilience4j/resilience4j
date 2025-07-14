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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.resilience4j.core.ExecutorServiceFactory;

/**
 * Creates threads using "$name-%d" pattern for naming. Is based on {@link Executors#defaultThreadFactory}
 */
public class NamingThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String prefix;

    public NamingThreadFactory(String name) {
        this.group = getThreadGroup();
        this.prefix = String.join("-",name, "");
    }

    /**
     * Returns the ThreadGroup to use for newly created threads.
     *
     * Historically this consulted {@link System#getSecurityManager()}, but the
     * SecurityManager API is deprecated for removal as of JDK 17.  All modern
     * applications run without a custom SecurityManager, so we now simply return
     * the current thread's group.
     */
    private ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = createName();
        Thread thread = ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL
            ? Thread.ofVirtual().name(name, 0).unstarted(runnable)
            : new Thread(group, runnable, name, 0);
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }

    private String createName() {
        return prefix + threadNumber.getAndIncrement();
    }
}
