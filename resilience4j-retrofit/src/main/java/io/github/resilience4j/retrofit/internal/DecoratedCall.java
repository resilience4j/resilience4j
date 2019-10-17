/*
 *
 *  Copyright 2017 Christopher Pilsworth
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
 *
 */
package io.github.resilience4j.retrofit.internal;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

/**
 * Simple decorator class that implements Call&lt;T&gt; and delegates all calls the the Call
 * instance provided in the constructor.  Methods can be overridden as required.
 *
 * @param <T> Call parameter type
 */
public abstract class DecoratedCall<T> implements Call<T> {

    private final Call<T> call;

    public DecoratedCall(Call<T> call) {
        this.call = call;
    }

    @Override
    public Response<T> execute() throws IOException {
        return this.call.execute();
    }

    @Override
    public void enqueue(Callback<T> callback) {
        call.enqueue(callback);
    }

    @Override
    public boolean isExecuted() {
        return call.isExecuted();
    }

    @Override
    public void cancel() {
        call.cancel();
    }

    @Override
    public boolean isCanceled() {
        return call.isCanceled();
    }

    @Override
    public abstract Call<T> clone();

    @Override
    public Request request() {
        return call.request();
    }
}
