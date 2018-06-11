package com.rapsealk.voicemotion.api

data class PredictResponse(
    val message: String,
    val predictions: Prediction
)

data class Prediction(
    val happy: Double,
    val neutral: Double,
    val sad: Double,
    val angry: Double
)