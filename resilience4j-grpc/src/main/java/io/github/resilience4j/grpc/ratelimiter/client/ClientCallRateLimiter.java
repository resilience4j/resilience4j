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
package io.github.resilience4j.grpc.ratelimiter.client;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptors;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;

public class ClientCallRateLimiter <ReqT, RespT>
    extends ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT> {

    private final RateLimiter rateLimiter;

    private ClientCallRateLimiter(ClientCall<ReqT, RespT> delegate, RateLimiter rateLimiter) {
        super(delegate);
        this.rateLimiter = rateLimiter;
    }

    public static <ReqT, RespT> ClientCallRateLimiter<ReqT, RespT> decorate(
        ClientCall<ReqT, RespT> delegate, RateLimiter rateLimiter) {
        return new ClientCallRateLimiter<>(delegate, rateLimiter);
    }

    private void acquirePermissionOrThrowStatus() throws StatusException {
        try {
            RateLimiter.waitForPermission(rateLimiter);
        } catch (Exception exception) {
            throw Status.UNAVAILABLE
                .withDescription(exception.getMessage())
                .withCause(exception)
                .asException();
        }
    }

    @Override
    protected void checkedStart(Listener<RespT> responseListener, Metadata headers) throws Exception {
        acquirePermissionOrThrowStatus();
        delegate().start(responseListener, headers);
    }
}
