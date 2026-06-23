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

import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TestContextPropagators {

    public static class TestThreadLocalContextPropagator implements ContextPropagator<String> {
        private ThreadLocal<String> threadLocal;

        public TestThreadLocalContextPropagator(ThreadLocal<String> threadLocal) {
            this.threadLocal = threadLocal;
        }

        @Override
        public Supplier<Optional<String>> retrieve() {
            return () -> (Optional<String>) Optional.ofNullable(threadLocal.get());
        }

        @Override
        public Consumer<Optional<String>> copy() {
            return (t) -> t.ifPresent(e -> {
                if (threadLocal.get() != null) {
                    threadLocal.set(null);
                    threadLocal.remove();
                }
                threadLocal.set(e);
            });
        }

        @Override
        public Consumer<Optional<String>> clear() {
            return (t) -> {
                if (threadLocal.get() != null) {
                    threadLocal.set(null);
                    threadLocal.remove();
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    public static class TestThreadLocalContextPropagatorWithHolder<T> implements ContextPropagator<T> {

        @Override
        public Supplier<Optional<T>> retrieve() {
            return () -> (Optional<T>) TestThreadLocalContextHolder.get();
        }

        @Override
        public Consumer<Optional<T>> copy() {
            return (t) -> t.ifPresent(e -> {
                clear();
                TestThreadLocalContextHolder.put(e);
            });
        }

        @Override
        public Consumer<Optional<T>> clear() {
            return t -> TestThreadLocalContextHolder.clear();
        }

        public static class TestThreadLocalContextHolder {

            private static final ThreadLocal<Object> threadLocal = new ThreadLocal<>();

            private TestThreadLocalContextHolder() {
            }

            public static void put(Object context) {
                if (threadLocal.get() != null) {
                    clear();
                }
                threadLocal.set(context);
            }
            public static Map<String, String> getMDCContext() {
                return Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(Collections.emptyMap());
            }

            public static void clear() {
                if (threadLocal.get() != null) {
                    threadLocal.set(null);
                    threadLocal.remove();
                }
            }

            public static Optional<?> get() {
                return Optional.ofNullable(threadLocal.get());
            }
        }
    }
}

