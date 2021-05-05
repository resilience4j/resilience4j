/*
 *  Copyright 2021 Ken Dombeck
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.github.resilience4j.grpc.ratelimiter.client.interceptor;

import io.github.resilience4j.grpc.ratelimiter.client.ClientCallRateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

public class ClientRateLimiterInterceptor implements ClientInterceptor {

    private final RateLimiter rateLimiter;

    private ClientRateLimiterInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public static ClientRateLimiterInterceptor of(RateLimiter rateLimiter) {
        return new ClientRateLimiterInterceptor(rateLimiter);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return ClientCallRateLimiter.decorate(
            next.newCall(method, callOptions), rateLimiter);
    }
}
