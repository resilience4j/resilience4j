package io.github.resilience4j.retrofit;

import retrofit2.Call;
import retrofit2.http.GET;

public interface RetrofitService {

    @GET("greeting")
    Call<String> greeting();

}