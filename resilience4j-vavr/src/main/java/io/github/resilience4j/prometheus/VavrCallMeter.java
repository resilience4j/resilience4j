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
package io.github.resilience4j.prometheus;

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;

public interface VavrCallMeter extends CallMeter {

    /**
     * Creates a timed checked supplier.
     *
     * @param meter    the call meter to use
     * @param supplier the original supplier
     * @return a timed supplier
     */
    static <T> CheckedFunction0<T> decorateCheckedSupplier(CallMeterBase meter,
                                                           CheckedFunction0<T> supplier) {
        return () -> {
            final Timer timer = meter.startTimer();
            try {
                final T returnValue = supplier.apply();
                timer.onSuccess();
                return returnValue;
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed runnable.
     *
     * @param meter    the call meter to use
     * @param runnable the original runnable
     * @return a timed runnable
     */
    static CheckedRunnable decorateCheckedRunnable(CallMeterBase meter, CheckedRunnable runnable) {
        return () -> {
            final Timer timer = meter.startTimer();
            try {
                runnable.run();
                timer.onSuccess();
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }

    /**
     * Creates a timed function.
     *
     * @param meter    the call meter to use
     * @param function the original function
     * @return a timed function
     */
    static <T, R> CheckedFunction1<T, R> decorateCheckedFunction(CallMeterBase meter,
                                                                 CheckedFunction1<T, R> function) {
        return (T t) -> {
            final Timer timer = meter.startTimer();
            try {
                R returnValue = function.apply(t);
                timer.onSuccess();
                return returnValue;
            } catch (Throwable e) {
                timer.onError();
                throw e;
            }
        };
    }
}
