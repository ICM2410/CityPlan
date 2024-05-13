package com.example.primeraentrega.Notifications

import java.time.LocalDateTime

class PlanState(
    var isEnteringToken: Boolean=true,
    var remoteToken:String="",
    var messageText:String="",
    var idPlan:String="",
    var idAlarm: Int=0,
    var idGrupo:String="",
) {
}