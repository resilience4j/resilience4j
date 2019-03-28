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
package io.github.resilience4j.recovery;

public final class DefaultRecoveryFunction<R> implements RecoveryFunction<R> {
    private static final DefaultRecoveryFunction<Object> INSTANCE = new DefaultRecoveryFunction<>();

    private DefaultRecoveryFunction() {}

    @Override
    public R apply(Throwable throwable) throws Throwable {
        throw  throwable;
    }

    @SuppressWarnings("unchecked")
    public static <T> DefaultRecoveryFunction<T> getInstance() {
        return (DefaultRecoveryFunction<T>) INSTANCE;
    }
}
