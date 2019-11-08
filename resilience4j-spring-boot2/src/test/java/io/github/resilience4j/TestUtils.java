package io.github.resilience4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;


public class TestUtils {

    public static void assertAnnotations(Class<?> originalClass, Class<?> onMissingBeanClass)
        throws NoSuchMethodException {
        for (Method methodBulkheadConfiguration : originalClass.getMethods()) {
            if (methodBulkheadConfiguration.isAnnotationPresent(Bean.class)) {
                final Method methodOnMissing = onMissingBeanClass
                    .getMethod(methodBulkheadConfiguration.getName(),
                        methodBulkheadConfiguration.getParameterTypes());

                assertThat(methodOnMissing.isAnnotationPresent(Bean.class)).isTrue();

                if (!methodOnMissing.getName().endsWith("RegistryEventConsumer")) {
                    assertThat(methodOnMissing.isAnnotationPresent(ConditionalOnMissingBean.class))
                        .isTrue();
                }

            }
        }
    }
}
