package com.example.primeraentrega.Notifications

import retrofit2.http.Body
import retrofit2.http.POST

interface FcmApi {
    //para enviar la info de planes
    //aca toca revisar como poner una alarma >:|
    @POST("/send")
    suspend fun sendMessage(
        @Body body: SendMessageDTO
    )

    @POST("/broadcast")
    suspend fun broadcast(
        @Body body: SendMessageDTO
    )
}
