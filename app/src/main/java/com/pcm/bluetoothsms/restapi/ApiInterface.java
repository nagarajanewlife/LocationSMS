package com.pcm.bluetoothsms.restapi;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by subbu on 17/7/17.
 */
public interface ApiInterface {

    @GET(ApiConstant.DATA_URL)
    Call<insertResponse> getURLResult(@Query("file") String file, @Query("data") String data);
}
