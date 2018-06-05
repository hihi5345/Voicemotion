package com.rapsealk.voicemotion.api

import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

public interface Api {

    companion object {

        public fun getInstance(address: String): Retrofit {
            return Retrofit.Builder()
                    .baseUrl("http://$address:3000/")
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
        }
    }

    @POST("predict")
    public fun getPredict(@Body body: PredictBody): Observable<PredictResponse>
}