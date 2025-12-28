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
package io.github.resilience4j.spring6.bulkhead.configure;

import org.springframework.core.Ordered;

public class BulkheadConfigurationProperties extends
    io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties {

    private int bulkheadAspectOrder = Ordered.LOWEST_PRECEDENCE - 1;

    /**
     * As of release 0.16.0 as we set an implicit spring aspect order for bulkhead to cover the
     * async case of threadPool bulkhead but user can override it still if he has different use case
     */
    public int getBulkheadAspectOrder() {
        return bulkheadAspectOrder;
    }

    /**
     * @param bulkheadAspectOrder bulkhead aspect order
     */
    public void setBulkheadAspectOrder(int bulkheadAspectOrder) {
        this.bulkheadAspectOrder = bulkheadAspectOrder;
    }

}
