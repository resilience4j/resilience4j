package io.github.resilience4j.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;


/**
 * the inject Rx java aspect support spring condition check
 */
public class RxJava3OnClasspathCondition implements Condition {

    private static final Logger logger = LoggerFactory.getLogger(RxJava3OnClasspathCondition.class);
    private static final String CLASS_TO_CHECK = "io.reactivex.rxjava3.core.Flowable";
    private static final String R4J_RXJAVA3 = "io.github.resilience4j.rxjava3.AbstractSubscriber";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return AspectUtil.checkClassIfFound(context, CLASS_TO_CHECK, (e) -> logger.debug(
                "RxJava3 related Aspect extensions are not activated, because RxJava3 is not on the classpath."))
                && AspectUtil.checkClassIfFound(context, R4J_RXJAVA3, (e) -> logger.debug(
                "RxJava3 related Aspect extensions are not activated because Resilience4j RxJava3 module is not on the classpath."));
    }
}
