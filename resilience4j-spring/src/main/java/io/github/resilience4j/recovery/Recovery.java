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
import org.springframework.util.StringUtils;

import java.util.List;

public class Recovery {
    private final List<RecoveryDecoratorGenerator> recoveryDecoratorGenerator;
    private final CheckedFunction1<CheckedFunction0<Object>, Object> NOOP_RECOVERY = CheckedFunction0::apply;
    private final RecoveryDecoratorGenerator defaultRecoveryDecoratorGenerator = new DefaultRecoveryDecoratorGenerator();

    public Recovery(List<RecoveryDecoratorGenerator> recoveryDecoratorGenerator) {
        this.recoveryDecoratorGenerator = recoveryDecoratorGenerator;
    }

    public CheckedFunction1<CheckedFunction0<Object>, Object> decorator(String recoveryMethodName, Object[] args, Class<?> returnType, Object target) {
        if (StringUtils.isEmpty(recoveryMethodName)) {
            return NOOP_RECOVERY;
        }

        return recoveryDecoratorGenerator.stream().filter(it -> it.supports(returnType))
                .findFirst()
                .map(it -> it.get(recoveryMethodName, args, target))
                .orElseGet(() -> defaultRecoveryDecoratorGenerator.get(recoveryMethodName, args, target));
    }
}
