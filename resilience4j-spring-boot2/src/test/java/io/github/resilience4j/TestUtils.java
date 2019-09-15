package io.github.resilience4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;


public class TestUtils {

	public static void assertAnnotations(Class<?> originalClass, Class<?> onMissingBeanClass) throws NoSuchMethodException {
		for (Method methodBulkheadConfiguration : originalClass.getMethods()) {
			if (methodBulkheadConfiguration.isAnnotationPresent(Bean.class)) {
				final Method methodOnMissing = onMissingBeanClass
						.getMethod(methodBulkheadConfiguration.getName(), methodBulkheadConfiguration.getParameterTypes());

				assertThat(methodOnMissing.isAnnotationPresent(Bean.class)).isTrue();

				if (!methodOnMissing.getName().endsWith("RegistryEventConsumer")) {
					assertThat(methodOnMissing.isAnnotationPresent(ConditionalOnMissingBean.class)).isTrue();
				}

			}
		}
	}
}
