/*
 *
 *  Copyright 2020: KrnSaurabh
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
package io.github.resilience4j.metrics;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;

public interface VavrTimer {

    /**
     * Creates a timed checked supplier.
     *
     * @param timer    the timer to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(Timer timer,
                                                           CheckedFunction0<T> supplier) {
        return () -> {
            final Timer.Context context = timer.context();
            try {
                T returnValue = supplier.apply();
                context.onSuccess();
                return returnValue;
            } catch (Throwable e) {
                context.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed runnable.
     *
     * @param timer    the timer to use
     * @param runnable the original runnable
     * @return a timed runnable
     */
    static CheckedRunnable decorateCheckedRunnable(Timer timer, CheckedRunnable runnable) {
        return () -> {
            final Timer.Context context = timer.context();
            try {
                runnable.run();
                context.onSuccess();
            } catch (Throwable e) {
                context.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed function.
     *
     * @param timer    the timer to use
     * @param function the original function
     * @return a timed function
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(Timer timer,
                                                                 CheckedFunction1<T, R> function) {
        return (T t) -> {
            final Timer.Context context = timer.context();
            try {
                R returnValue = function.apply(t);
                context.onSuccess();
                return returnValue;
            } catch (Throwable e) {
                context.onError();
                throw e;
            }
        };
    }
}
