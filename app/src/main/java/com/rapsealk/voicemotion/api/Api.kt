package com.rapsealk.voicemotion.api

import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

public interface Api {

    companion object {

        public val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.35.9:3000/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    @POST("predict")
    public fun getPredict(@Body body: PredictBody): Observable<PredictResponse>
}