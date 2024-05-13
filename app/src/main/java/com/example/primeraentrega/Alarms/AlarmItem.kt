package com.example.primeraentrega.Alarms

import java.time.LocalDateTime

class AlarmItem (
    val time:  LocalDateTime,
    val message: String,
    val nombreplan: String,
    var idPlan: Int,
    val idPlanReal: String,
    val idGrupo: String
)