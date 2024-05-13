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

    //para enviar la info de chats
    @POST("/send/chat")
    suspend fun sendMessageChat(
        @Body body: SendMessageDTO
    )

    @POST("/broadcast/chat")
    suspend fun broadcastChat(
        @Body body: SendMessageDTO
    )

    //para enviar la info de grupos
    @POST("/send/group")
    suspend fun sendMessageGroup(
        @Body body: SendMessageDTO
    )

    @POST("/broadcast/group")
    suspend fun broadcastGroup(
        @Body body: SendMessageDTO
    )
}
