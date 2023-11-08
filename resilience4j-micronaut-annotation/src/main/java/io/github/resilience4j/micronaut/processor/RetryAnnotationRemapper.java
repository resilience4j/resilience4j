/*
 * Copyright 2023 original authors
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
package io.github.resilience4j.micronaut.processor;

import io.micronaut.inject.annotation.PackageRenameRemapper;

/**
 * Allows using either resilience4j annotations or the Micronaut versions.
 */
public class RetryAnnotationRemapper implements PackageRenameRemapper {

    @Override
    public String getTargetPackage() {
        return "io.github.resilience4j.micronaut.annotation";
    }

    @Override
    public String getPackageName() {
        return "io.github.resilience4j.retry.annotation";
    }
}
