package io.github.resilience4j.ratelimiter.response;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.server.exceptions.ExceptionHandler;

import javax.inject.Singleton;

/**
 * Exception handler for {@link RequestNotPermitted}.
 *
 * @author James Kleeh
 * @since 1.0.0
 */
@Singleton
public class RequestNotPermittedHandler implements ExceptionHandler<RequestNotPermitted, HttpResponse<?>> {

    @Override
    public HttpResponse<?> handle(HttpRequest request, RequestNotPermitted exception) {
        return HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS)
            .contentType(MediaType.TEXT_PLAIN_TYPE)
            .body(exception.getMessage());
    }
}
