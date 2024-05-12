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

        if (intent != null) {
            when (intent.action) {
                ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                    Toast.makeText(context, "RECEIVED ALARM PERMISSION", Toast.LENGTH_LONG).show()
                }
                "from alarm" -> {
                    Toast.makeText(context, "ALARM FIRED", Toast.LENGTH_LONG).show()
                }
            }
        }
        val message=intent?.getStringExtra("message")
        val idPlan=intent?.getStringExtra("idPlan")
        val nombre=intent?.getStringExtra("nombre")
        val idGrupo=intent?.getStringExtra("idGrupo")
        val idPlanReal=intent?.getStringExtra("idPlanReal")

        Log.e("ALARMA","ALARMA DEL PLAN $nombre con id $idPlan")
        //AQUI SE PONE LA NOTIFICACION PARA EL PLAN
         context?.let {
             val notification =NotificationCompat. Builder (it,"disponible")
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
            notification.addAction(R.drawable. ic_launcher_background,"ver ubicacion", mapPendingIntent)
            notification.setContentIntent(mapPendingIntent)

             if (ActivityCompat.checkSelfPermission(
                     context,
                     Manifest.permission.POST_NOTIFICATIONS
                 ) != PackageManager.PERMISSION_GRANTED
             ) {
                 // TODO: Consider calling
                 //    ActivityCompat#requestPermissions
                 // here to request the missing permissions, and then overriding
                 //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                 //                                          int[] grantResults)
                 // to handle the case where the user grants the permission. See the documentation
                 // for ActivityCompat#requestPermissions for more details.
                 return
             }
             val notificationManagerCompat = NotificationManagerCompat.from(context)
             notificationManagerCompat.notify(123, notification.build())

         }
    }
}
