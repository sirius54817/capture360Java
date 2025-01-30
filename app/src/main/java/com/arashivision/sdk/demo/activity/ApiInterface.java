package com.arashivision.sdk.demo.activity;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiInterface {
    @FormUrlEncoded
    @POST("/api/users")
    abstract Call<User> getUserInformation(@Field("name") String name, @Field("job") String job);

}
