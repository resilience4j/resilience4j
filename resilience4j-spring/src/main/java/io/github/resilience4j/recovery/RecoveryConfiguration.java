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

import io.github.resilience4j.utils.ReactorOnClasspathCondition;
import io.github.resilience4j.utils.RxJava2OnClasspathCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * {@link Configuration
 * Configuration} for {@link Recovery}.
 */
@Configuration
public class RecoveryConfiguration {

	@Bean
	@Conditional(value = {RxJava2OnClasspathCondition.class})
	public RecoveryApplier rxJava2RecoveryApplier() {
		return new RxJava2RecoveryApplier();
	}

	@Bean
	@Conditional(value = {ReactorOnClasspathCondition.class})
	public RecoveryApplier reactorRecoveryApplier() {
		return new ReactorRecoveryApplier();
	}

	@Bean
	public RecoveryApplier completionStageRecoveryApplier() {
		return new CompletionStageRecoveryApplier();
	}

	@Bean
	public Recovery recovery(List<RecoveryApplier> recoveryApplier) {
		return new Recovery(recoveryApplier);
	}
}
