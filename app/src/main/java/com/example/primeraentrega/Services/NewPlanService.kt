package com.example.primeraentrega.Services

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.primeraentrega.Alarms.AlarmItem
import com.example.primeraentrega.Alarms.AndroidAlarmScheduler
import com.example.primeraentrega.Clases.Grupo
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class NewPlanService: Service() {
    private val serviceScope = CoroutineScope(SupervisorJob())
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var uid=""
    private lateinit var scheduler: AndroidAlarmScheduler
    var alarmItem:AlarmItem?=null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        databaseReference = FirebaseDatabase.getInstance().getReference("Groups")
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        scheduler=AndroidAlarmScheduler(this)
    }

    private val gruposAnadidos = HashSet<Int>()
    private fun subscribirseACambiosDelUsuario() {
        Log.i("service mis grupos","ENTRE")
        val query = databaseReference
        query.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                //for (userSnapshot in dataSnapshot.children) {
                    Log.i("service mis grupos","me anadieron a un nuevo grupo $dataSnapshot")

                    val groupId = dataSnapshot.key // El ID del usuario
                    val grupoData = dataSnapshot.getValue(Grupo::class.java)
                    if (grupoData != null) {
                        if(grupoData.integrantes.containsKey(uid)) {
                            //recorrer los planes
                            Log.i("service mis grupos","me anadieron a un nuevo grupo")

                            //subscribirme a su canal de notificaciones
                            if (groupId != null) {
                                subscribirACanal(groupId)
                            }
                            for(plan in grupoData.planes)
                            {
                                if(!gruposAnadidos.contains(plan.value.idAlarma))
                                {
                                    //gruposAnadidos.add(plan.value.idAlarma)

                                    val dateInicio = plan.value.dateInicio
                                    val dateFin = plan.value.dateFinal


                                    if (dateInicio != null && dateFin != null) {
                                        // Configura el formato de fecha y hora
                                        val formatoFecha = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                                        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

                                        // Establece la zona horaria a UTC si es necesario
                                        formatoFecha.timeZone = TimeZone.getTimeZone("UTC")
                                        formatoHora.timeZone = TimeZone.getTimeZone("UTC")
                                        formatoFecha.format(dateInicio)
                                        formatoHora.format(dateInicio)
                                        val horaActual = LocalDateTime.now()
                                        val fechaHoraAlarma =textoAFechaAlarma(
                                            formatoFecha.format(dateInicio).toString(),
                                            formatoHora.format(dateInicio).toString()
                                        )
                                        //if(fechaHoraAlarma.isAfter(horaActual))
                                        //{
                                            Log.i("alarma","asignaron una nueva alarma")
                                            //agendar la alarma
                                            alarmItem= groupId?.let {
                                                AlarmItem(
                                                    textoAFechaAlarma(
                                                        formatoFecha.format(dateInicio).toString(),
                                                        formatoHora.format(dateInicio).toString()
                                                    ),
                                                    //textoAFechaAlarma(binding.fechaInicio, binding.horaInicio),
                                                    "El plan ${plan.value.titulo} ha iniciado",
                                                    plan.value.titulo,
                                                    plan.value.idAlarma,
                                                    plan.value.id,
                                                    it
                                                )
                                            }

                                            alarmItem?.let (scheduler::schedule)
                                        gruposAnadidos.add(plan.value.idAlarma)
                                        //}
                                    }
                                }
                            }
                        }
                    }
               // }
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            @SuppressLint("ForegroundServiceType")
            override fun onChildChanged(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                //val grupo = dataSnapshot.getValue(Grupo::class.java)
                //for (userSnapshot in dataSnapshot.children) {
                    Log.i("service mis grupos","me anadieron a un nuevo grupo $dataSnapshot")

                    val groupId = dataSnapshot.key // El ID del usuario
                    val grupoData = dataSnapshot.getValue(Grupo::class.java) // Los datos del usuario convertidos a objeto Usuario
                    if (grupoData != null) {
                        if(grupoData.integrantes.containsKey(uid)) {
                            //recorrer los planes
                            //Log.i("services mis grupos","cambiaron el grupo: $dataSnapshot")
                            for(plan in grupoData.planes)
                            {
                                Log.i("services mis grupos","hay un plan: $dataSnapshot")
                                if(!gruposAnadidos.contains(plan.value.idAlarma))
                                {
                                    Log.i("services mis grupos","no existe la alarma: $dataSnapshot")

                                    val dateInicio = plan.value.dateInicio
                                    val dateFin = plan.value.dateFinal


                                    if (dateInicio != null && dateFin != null) {
                                        // Configura el formato de fecha y hora
                                        val formatoFecha = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                                        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

                                        // Establece la zona horaria a UTC si es necesario
                                        formatoFecha.timeZone = TimeZone.getTimeZone("UTC")
                                        formatoHora.timeZone = TimeZone.getTimeZone("UTC")
                                        val horaActual = LocalDateTime.now()
                                        val fechaHoraAlarma =textoAFechaAlarma(
                                            formatoFecha.format(dateInicio).toString(),
                                            formatoHora.format(dateInicio).toString()
                                        )

                                        Log.i("alarma","asignaron una nueva alarma")
                                        //agendar la alarma
                                        alarmItem= groupId?.let {
                                            AlarmItem(
                                                textoAFechaAlarma(
                                                    formatoFecha.format(dateInicio).toString(),
                                                    formatoHora.format(dateInicio).toString()
                                                ),
                                                //textoAFechaAlarma(binding.fechaInicio, binding.horaInicio),
                                                "El plan ${plan.value.titulo} ha iniciado",
                                                plan.value.titulo,
                                                plan.value.idAlarma,
                                                plan.value.id,
                                                it
                                            )
                                        }

                                        alarmItem?.let (scheduler::schedule)
                                        gruposAnadidos.add(plan.value.idAlarma)

                                    }
                                }
                            }
                        }
                    }
               // }

            }
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }
            override fun onChildMoved(dataSnapshot: DataSnapshot, prevChildKey: String?) {
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    private fun subscribirACanal(canal:String) {
        //aqui se debe subscribir a todos los chats a los que pertenece
        Firebase.messaging.subscribeToTopic(canal).addOnSuccessListener {
            Log.i("subscripcion", "Existosa")
        }.addOnFailureListener{
            Log.e("subscripcion", "ERROR")
        }
    }

    fun textoAFechaAlarma(fechaTexto: String, horaTexto: String): LocalDateTime {
        // Parsear los textos de fecha y hora en LocalDateTime
        val formatter = DateTimeFormatter.ofPattern("M/d/yyyy HH:mm")

        // Parsear los textos de fecha y hora en LocalDateTime
        val fechaHora = LocalDateTime.parse("${fechaTexto} ${horaTexto}", formatter)
        Log.i("tiempo","es: $fechaHora")
        // Calcular la diferencia en segundos entre la hora actual y la fechaHora propuesta
        val diferenciaSegundos = LocalDateTime.now().until(fechaHora, java.time.temporal.ChronoUnit.SECONDS)
        Log.i("tiempo","es: diferencias local ${LocalDateTime.now()} con  inicio $diferenciaSegundos")
        // Ajustar la hora actual sumando la diferencia en segundos
        return LocalDateTime.now().plusSeconds(diferenciaSegundos)
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            val dato = it.getStringExtra("uid").toString()
            if (dato != "null") {
                // Hacer algo con el dato recibido
                Log.i("TuServicio", "Dato recibido: $dato")
                uid=dato
            }
        }
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Groups")
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        subscribirseACambiosDelUsuario()
        scheduler= AndroidAlarmScheduler(this)
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

    }
}