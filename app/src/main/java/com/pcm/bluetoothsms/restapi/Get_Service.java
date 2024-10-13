package com.pcm.bluetoothsms.restapi;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by vikram on 19/7/17.
 */

public class Get_Service {
    private static final String TAG = Get_Service.class.getSimpleName();
    private ApiInterface mInterface;
    private Call<insertResponse> reqData;
    private Data_listener mCallback;

    public Get_Service(AppCompatActivity mActivity,Data_listener listener) {
        this.mCallback = listener;
        mInterface = ApiClient.getClient(mActivity).create(ApiInterface.class);
    }

    public void getData(String data,String file) {
        Log.d(TAG, "getCustomerRemarksData");
        reqData = mInterface.getURLResult(file,data);
        try {
            reqData.enqueue(new Callback<insertResponse>() {
                @Override
                public void onResponse(Call<insertResponse> call, Response<insertResponse> response) {
                    Log.d(TAG, "onResponse");
                    if (response.isSuccessful()) {
                        mCallback.onReceivedsuccess(response.body().getMessage());
                    } else mCallback.onReceivedError(response.message());
                }

                @Override
                public void onFailure(Call<insertResponse> call, Throwable t) {
                    Log.d(TAG, "onFailure" + t.getMessage());
                    mCallback.onReceivedError("request error occurred");
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public interface Data_listener {
        void onReceivedsuccess(String msg);

        void onReceivedError(String msg);
    }
}

