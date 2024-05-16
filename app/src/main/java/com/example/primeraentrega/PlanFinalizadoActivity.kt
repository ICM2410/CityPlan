package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.primeraentrega.Adapters.AdapterEstadistica
import com.example.primeraentrega.Adapters.Adapterplan
import com.example.primeraentrega.Clases.Estadistica
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.Clases.PlanLista
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.databinding.ActivityPlanFinalizadoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PlanFinalizadoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanFinalizadoBinding

    private var isFabOpen=false
    private var rotation=false
    private lateinit var idGrupo : String
    private var idPlan : String=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanFinalizadoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        idGrupo=intent.getStringExtra("idGrupo").toString()
        idPlan=intent.getStringExtra("idPlan").toString()
        inicializarBotones()
        obtenerEstadisticas()
    }

    val estadisticas: MutableList<Estadistica> = mutableListOf()
    private fun obtenerEstadisticas() {
        val estadisticaRef= FirebaseDatabase.getInstance().getReference("Estadisticas")
            .child(idPlan)

        estadisticaRef.addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (userSnapshot in dataSnapshot.children) {
                        // Obtiene los datos de cada usuario
                        val estId = userSnapshot.key // El ID del usuario
                        val estData = userSnapshot.getValue(Estadistica::class.java) // Los datos del usuario convertidos a objeto Usuario

                        // Aquí puedes realizar cualquier operación con los datos del usuario
                        println("ID de est: $estId")
                        println("Datos de est: $estData")

                        // Crea un objeto PosAmigo con la información del usuario

                        if (estData != null) {
                            estadisticas.add(estData)
                        }

                    }
                    //poner los planes e la list view
                    var adapter = AdapterEstadistica(applicationContext,estadisticas);
                    binding.listaEst.adapter = adapter
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    // Maneja el error en caso de que ocurra
                    println("Error al obtener los datos de planes: ${databaseError.message}")
                }
            })
    }

    private fun inicializarBotones() {

        binding.botonVerRecuerdos.setOnClickListener {
            var intent = Intent(baseContext, GaleriaActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        val usuario: UsuarioAmigo = UsuarioAmigo()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    startActivity(Intent(baseContext, VerGruposActivity::class.java))
                    true
                }
                R.id.cuenta_bar -> {
                    val executor = ContextCompat.getMainExecutor(this)
                    val biometricPrompt = BiometricPrompt(this, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                // Aquí puedes realizar alguna acción después de la autenticación exitosa
                                // Por ejemplo, mostrar un mensaje o iniciar una nueva actividad
                                var intent = Intent(baseContext, PerfilConfActivity::class.java)
                                intent.putExtra("user", usuario)
                                startActivity(intent)
                                //startActivity(Intent(baseContext, PerfilConfActivity::class.java))
                                //startActivity(Intent(baseContext, VerGruposActivity::class.java))
                                true
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Autenticación de huella dactilar")
                        .setSubtitle("Toque el sensor de huella dactilar")
                        .setNegativeButtonText("Cancelar")
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                    // Respond to navigation item 2 click
                    false
                }
                R.id.salir_bar -> {
                    // Respond to navigation item 3 click
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                    true
                }
                else -> false
            }
        }

        initShowout(binding.pasadosView)
        initShowout(binding.nuevoView)
        initShowout(binding.activoView)
        initShowout(binding.planesView)
        binding.fabMenuPlan.setOnClickListener {
            if(!isFabOpen)
            {
                showFabMenu();
            }
            else
            {
                closeFabMenu();
            }
        }

        fabClicks()
    }

    private fun fabClicks() {
        binding.fabPlanesPasados.setOnClickListener {
            var intent = Intent(baseContext, PlanesPasadosActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabCrearPlan.setOnClickListener {
            var intent = Intent(baseContext, CrearPlanActivity::class.java)
            intent.putExtra("pantalla", "planes")
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabMisPlanes.setOnClickListener {
            var intent = Intent(baseContext, PlanesActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabPlanActivo.setOnClickListener {
            revisarActivo()
        }
    }

    private fun revisarActivo() {
        var existe=false
        val ref = FirebaseDatabase.getInstance().getReference("Groups")
        ref.child(idGrupo).child("planes").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    // Obtiene los datos de cada usuario
                    val planId = userSnapshot.key // El ID del usuario
                    val planData = userSnapshot.getValue(Plan::class.java) // Los datos del usuario convertidos a objeto Usuario

                    // Aquí puedes realizar cualquier operación con los datos del usuario
                    println("ID de usuario: $planId")
                    println("Datos de usuario: $planData")

                    // Crea un objeto PosAmigo con la información del usuario
                    var status=""
                    val plan = planData?.let {
                        status=planAcrivo(planData.dateInicio,planData.dateFinal)
                    }

                    // Si el usuario y su ID no son nulos, añádelos al mapa integrantesMap
                    if (planId != null &&  plan != null && status!="Activo") {
                        existe=true
                        idPlan=planId
                    }
                }

                if(existe)
                {
                    var intent = Intent(baseContext, PlanActivity::class.java)
                    intent.putExtra("idGrupo", idGrupo)
                    intent.putExtra("idPlan", idPlan)
                    startActivity(intent)
                }
                else
                {
                    Toast.makeText(applicationContext, "No hay planes activos", Toast.LENGTH_LONG).show()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Maneja el error en caso de que ocurra
                println("Error al obtener los datos de planes: ${databaseError.message}")
            }
        })
    }

    private fun planAcrivo(dateInicio: java.util.Date, dateFinal: java.util.Date): String {
        val fechaActual = LocalDateTime.now()

        val formatoFecha = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Establece la zona horaria a UTC si es necesario
        formatoFecha.timeZone = TimeZone.getTimeZone("UTC")
        formatoHora.timeZone = TimeZone.getTimeZone("UTC")

        val fechaHoraAlarmaInicio =textoAFechaAlarma(
            formatoFecha.format(dateInicio).toString(),
            formatoHora.format(dateInicio).toString()
        )

        val fechaHoraAlarmaFinal =textoAFechaAlarma(
            formatoFecha.format(dateFinal).toString(),
            formatoHora.format(dateFinal).toString()
        )

        return when {
            fechaActual<fechaHoraAlarmaInicio -> "Activo"
            fechaActual>fechaHoraAlarmaFinal -> "Cerrado"
            fechaActual>fechaHoraAlarmaInicio && fechaActual<fechaHoraAlarmaFinal-> "Abierto"
            else ->"Abierto"
        }
    }

    fun textoAFechaAlarma(fechaTexto: String, horaTexto: String): LocalDateTime {
        // Parsear los textos de fecha y hora en LocalDateTime
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")

        // Parsear los textos de fecha y hora en LocalDateTime
        val fechaHora = LocalDateTime.parse("${fechaTexto} ${horaTexto}", formatter)
        Log.i("tiempo","es: $fechaHora")
        // Calcular la diferencia en segundos entre la hora actual y la fechaHora propuesta
        val diferenciaSegundos = LocalDateTime.now().until(fechaHora, java.time.temporal.ChronoUnit.SECONDS)
        Log.i("tiempo","es: diferencias local ${LocalDateTime.now()} con  inicio $diferenciaSegundos")
        // Ajustar la hora actual sumando la diferencia en segundos
        return LocalDateTime.now().plusSeconds(diferenciaSegundos)
    }

    private fun initShowout (v: View){
        v.apply {
            visibility = View. GONE
            translationY= height.toFloat()
            alpha = 0f
        }
    }

    private fun closeFabMenu() {
        rotation=rotateFAB()
        isFabOpen=false
        cerrar(binding.pasadosView)
        cerrar(binding.nuevoView)
        cerrar(binding.activoView)
        cerrar(binding.planesView)
    }

    private fun cerrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=1f
            translationY=0f
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        visibility= View.GONE
                    }
                })
                .alpha(0f)
                .start()
        }
    }

    private fun showFabMenu() {
        rotation=rotateFAB()
        isFabOpen=true

        //motrar info
        mostrar(binding.pasadosView)
        mostrar(binding.nuevoView)
        mostrar(binding.activoView)
        mostrar(binding.planesView)

    }

    private fun mostrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=0f
            translationY=height.toFloat()
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){})
                .alpha(1f)
                .start()
        }
    }

    private fun rotateFAB():Boolean {
        binding.fabMenuPlan.animate()
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter(){})
            .rotation(if(!isFabOpen) 180f else 0f)

        return isFabOpen
    }
}