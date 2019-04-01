package io.github.resilience4j.circuitbreaker.configure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;


/**
 * @author romeh
 */
public class InjectReactorAspect implements Condition {

	private static final Logger logger = LoggerFactory.getLogger(InjectReactorAspect.class);

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		try {
			final Class<?> aClass = context.getClassLoader().loadClass("reactor.core.publisher.Flux");
			return aClass != null;
		} catch (ClassNotFoundException e) {
			logger.warn(e.getLocalizedMessage());
			return false;
		}
	}
}
