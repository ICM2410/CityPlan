package com.example.primeraentrega.Alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import java.time.ZoneId

class AndroidAlarmScheduler(
    private val context:Context
): AlarmScheduler {

    private val alarmManager=context.getSystemService(AlarmManager::class.java)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun schedule(item: AlarmItem) {

        val intent= Intent(context,AlarmReceiver::class.java).apply {
            putExtra("message",item.message)
            putExtra("idPlan", item.idAlarma.toString())
            putExtra("nombre",item.nombreplan)
            putExtra("idGrupo",item.idGrupo)
            putExtra("idPlanReal",item.idPlanReal)
        }


        try {
            Log.i("scheduler","se puso la alarma")
            // Tu código para programar la alarma exacta
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                item.time.atZone(ZoneId.systemDefault()).toEpochSecond()*1000,
                PendingIntent.getBroadcast(
                    context,
                    item.idAlarma,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } catch (e: SecurityException) {
            // Manejar la excepción y tomar acciones alternativas
            Log.i("alarma","no hay permiso de alarma")
        }

    }

    override fun cancel(item: AlarmItem) {
       alarmManager.cancel(
           PendingIntent.getBroadcast(
               context,
               item.hashCode(),
               Intent(context, AlarmReceiver::class.java),
               PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
           )
       )
    }

}