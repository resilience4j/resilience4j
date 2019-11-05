/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.retry.configure;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;
import io.vavr.CheckedFunction0;

/**
 * Retry aspect extension support interface type if you want to support new types
 */
public interface RetryAspectExt {

	boolean canHandleReturnType(Class returnType);

	CheckedFunction0<Object> decorate(ProceedingJoinPointHelper joinPointHelper, Retry retry);
}
