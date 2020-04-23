package io.github.resilience4j.ratelimiter.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterSupport;
import io.github.resilience4j.ratelimiter.operator.RateLimiterOperator;
import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.OncePerRequestHttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.server.util.HttpClientAddressResolver;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import java.util.Optional;

@Internal
@Filter("/**")
public class RateLimiterFilter extends OncePerRequestHttpServerFilter {

    private final RateLimiterRegistry registry;
    private final RateLimiterSupport rateLimiterSupport;
    private final HttpClientAddressResolver clientAddressResolver;

    RateLimiterFilter(HttpClientAddressResolver clientAddressResolver, RateLimiterRegistry registry, RateLimiterSupport rateLimiterSupport) {
        this.clientAddressResolver = clientAddressResolver;
        this.registry = registry;
        this.rateLimiterSupport = rateLimiterSupport;
    }

    @Override
    protected Publisher<MutableHttpResponse<?>> doFilterOnce(HttpRequest<?> request, ServerFilterChain chain) {
        if (rateLimiterSupport.shouldLimit(request)) {
            String key = clientAddressResolver.resolve(request);
            Optional<String> configName = rateLimiterSupport.getConfigurationName(request);
            RateLimiter rateLimiter = configName.map(name -> registry.rateLimiter(key, name)).orElseGet(() -> registry.rateLimiter(key));

            return Flowable.fromPublisher(chain.proceed(request))
                .compose(RateLimiterOperator.of(rateLimiter));
        } else {
            return chain.proceed(request);
        }
    }
}
