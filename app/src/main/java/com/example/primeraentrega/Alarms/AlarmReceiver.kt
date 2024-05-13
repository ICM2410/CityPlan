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
import com.example.primeraentrega.IniciarSesionActivity
import com.example.primeraentrega.PlanActivity
import com.example.primeraentrega.R
import com.google.firebase.auth.FirebaseAuth

class AlarmReceiver : BroadcastReceiver() {
    private var auth: FirebaseAuth=FirebaseAuth.getInstance()
    override fun onReceive(context: Context?, intent: Intent?) {

        val message=intent?.getStringExtra("message")
        val idAlarma=intent?.getStringExtra("idPlan")
        val nombre=intent?.getStringExtra("nombre")
        val idGrupo=intent?.getStringExtra("idGrupo")
        val idPlanReal=intent?.getStringExtra("idPlanReal")

        Log.e("ALARMA","ALARMA DEL PLAN $nombre con id $idAlarma")
        //AQUI SE PONE LA NOTIFICACION PARA EL PLAN
         context?.let {
             val notification =NotificationCompat. Builder (it,"notificacion")
                .setContentTitle("$nombre")
                .setContentText("$message")
                .setSmallIcon(R.drawable. ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

             if(auth.currentUser!=null){
                 val planIntent=Intent(context, PlanActivity::class.java)
                 planIntent.putExtra("idPlan",idPlanReal)
                 planIntent.putExtra("idGrupo",idGrupo)
                 planIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                 val mapPendingIntent = PendingIntent.getActivity( context, 1, planIntent,
                     PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                 notification.addAction(R.drawable. ic_launcher_background,"Ver plan!", mapPendingIntent)
                 notification.setContentIntent(mapPendingIntent)

             }
             else
             {
                 val signIntent=Intent(context, IniciarSesionActivity::class.java)
                 signIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                 val signPendingIntent = PendingIntent.getActivity( context, 1, signIntent,
                     PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                 notification.addAction(R.drawable. ic_launcher_background,"Ver plan!", signPendingIntent)
                 notification.setContentIntent(signPendingIntent)
             }


             val notificationManagerCompat = NotificationManagerCompat.from(context)

             if (ActivityCompat.checkSelfPermission(
                     context,
                     Manifest.permission.POST_NOTIFICATIONS
                 ) != PackageManager.PERMISSION_GRANTED
             ) {
                 Log.e("f","sin notificacion, permiso denegado por el usuario")
                 return
             }
             else
             {
                 if (idAlarma != null) {
                     notificationManagerCompat.notify(idAlarma.toInt(), notification.build())
                 }
             }

         }
    }
}
