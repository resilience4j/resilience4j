/*
 * Copyright 2019 Yevhenii Voievodin
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
package io.github.resilience4j.prometheus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Common constants for metric binder implementations based on tags. */
public final class LabelNames {

    private LabelNames() {}

    public static final List<String> NAME = Collections.singletonList("name");
    public static final List<String> NAME_AND_KIND = Arrays.asList("name", "kind");

}
