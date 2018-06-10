package com.rapsealk.voicemotion.api

import android.util.Log
import io.reactivex.Observable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

public interface Api {

    companion object {

        private val okHttpClient = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

        public fun getInstance(address: String): Retrofit {
            Log.d("API", "address: $address")
            return Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl("http://$address:3000/")
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
        }
    }

    @POST("predict")
    public fun getPredict(@Body body: PredictBody): Observable<PredictResponse>
}