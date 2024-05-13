package com.example.primeraentrega.Alarms

interface AlarmScheduler {
    fun schedule(item:AlarmItem)
    fun cancel(item:AlarmItem)
}