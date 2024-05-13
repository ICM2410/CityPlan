package com.example.primeraentrega.Notifications

import java.time.LocalDateTime

data class SendMessageDTO (
    val to: String?,
    val notification: NotificationBody
)

data class NotificationBody(
    val title: String,
    val body: String,
    val id:String,
    val alarmId:Int,
    val idGrupo:String
)