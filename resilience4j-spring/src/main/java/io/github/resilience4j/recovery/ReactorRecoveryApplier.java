/*
 * Copyright 2019 Kyuhyen Hwang
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

import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.function.Function;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

public class ReactorRecoveryApplier implements RecoveryApplier {
    private static final Set<Class> REACTORS_SUPPORTED_TYPES = newHashSet(Mono.class, Flux.class);

    @Override
    public boolean supports(Class target) {
        return REACTORS_SUPPORTED_TYPES.stream().anyMatch(it -> it.isAssignableFrom(target));
    }

    @Override
    public CheckedFunction1<CheckedFunction0<Object>, Object> get(String recoveryMethodName, Object[] args, Object target) {
        return (supplier) -> {
            Object returnValue = supplier.apply();
            if (Flux.class.isAssignableFrom(returnValue.getClass())) {
                Flux fluxReturnValue = (Flux) returnValue;
                return fluxReturnValue.onErrorResume(reactorOnErrorResume(recoveryMethodName, args, target, Flux::error));
            } else if (Mono.class.isAssignableFrom(returnValue.getClass())) {
                Mono monoReturnValue = (Mono) returnValue;
                return monoReturnValue.onErrorResume(reactorOnErrorResume(recoveryMethodName, args, target, Mono::error));
            } else {
                return returnValue;
            }
        };
    }

    private <T> Function<? super Throwable, ? extends Publisher<? extends T>> reactorOnErrorResume(String recoveryMethodName, Object[] args, Object target, Function<? super Throwable, ? extends Publisher<? extends T>> errorFunction) {
        return (throwable) -> {
            try {
                return (Publisher<? extends T>) invoke(recoveryMethodName, args, throwable, target);
            } catch (Throwable recoverThrowable) {
                return errorFunction.apply(recoverThrowable);
            }
        };
    }
}
