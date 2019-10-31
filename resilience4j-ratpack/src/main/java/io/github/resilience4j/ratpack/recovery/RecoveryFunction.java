/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.ratpack.recovery;

import ratpack.func.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface RecoveryFunction<O> extends Function<Throwable, O> {

    default Flux<? super O> onErrorResume(Flux<? super O> flux) {
        return flux.onErrorResume(t -> {
            O fallbackValue;
            try {
                Throwable actual = Optional.ofNullable(t.getCause()).orElse(t);
                fallbackValue = apply(actual);
                if (fallbackValue instanceof Flux) {
                    return (Flux) fallbackValue;
                }
            } catch (Exception e) {
                return Flux.error(e);
            }
            return Flux.just(fallbackValue);
        });
    }

    default Mono<? super O> onErrorResume(Mono<? super O> mono) {
        return mono.onErrorResume(t -> {
            O fallbackValue;
            try {
                Throwable actual = Optional.ofNullable(t.getCause()).orElse(t);
                fallbackValue = apply(actual);
            } catch (Exception e) {
                return Mono.error(e);
            }
            return Mono.just(fallbackValue);
        });
    }

}
