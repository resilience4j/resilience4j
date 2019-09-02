package io.github.resilience4j.retry.autoconfigure;

import io.github.resilience4j.retry.Retry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({Retry.class, RefreshScope.class})
@AutoConfigureAfter(RefreshAutoConfiguration.class)
@AutoConfigureBefore(RetryAutoConfiguration.class)
public class RefreshScopedRetryAutoConfiguration extends AbstractRefreshScopedRetryConfiguration {

}
