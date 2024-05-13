package com.example.primeraentrega.Alarms

import android.Manifest
import android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.primeraentrega.PlanActivity
import com.example.primeraentrega.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val message=intent?.getStringExtra("message")
        val idPlan=intent?.getStringExtra("idPlan")
        val nombre=intent?.getStringExtra("nombre")
        val idGrupo=intent?.getStringExtra("idGrupo")
        val idPlanReal=intent?.getStringExtra("idPlanReal")

        Log.e("ALARMA","ALARMA DEL PLAN $nombre con id $idPlan")
        //AQUI SE PONE LA NOTIFICACION PARA EL PLAN
         context?.let {
             val notification =NotificationCompat. Builder (it,"notificacion")
                .setContentTitle("$nombre")
                .setContentText("$message")
                .setSmallIcon(R.drawable. ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val planIntent=Intent(context, PlanActivity::class.java)
            planIntent.putExtra("idPlan",idPlanReal)
            planIntent.putExtra("idGrupo",idGrupo)
            planIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val mapPendingIntent = PendingIntent.getActivity( context, 1, planIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            notification.addAction(R.drawable. ic_launcher_background,"Ver plan!", mapPendingIntent)
            notification.setContentIntent(mapPendingIntent)

             val notificationManagerCompat = NotificationManagerCompat.from(context)

             if (ActivityCompat.checkSelfPermission(
                     context,
                     Manifest.permission.POST_NOTIFICATIONS
                 ) != PackageManager.PERMISSION_GRANTED
             ) {
                 Log.e("f","sin notificacion")
                 return
             }
             else
             {
                 notificationManagerCompat.notify(123, notification.build())
             }

         }
    }
}
