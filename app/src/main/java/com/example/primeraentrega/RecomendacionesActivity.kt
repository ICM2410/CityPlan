package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.primeraentrega.Clases.Establecimiento
import com.example.primeraentrega.Adapters.AdapterEstablecimiento
import com.example.primeraentrega.databinding.ActivityRecomendacionesBinding
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONException
import java.util.Date


class RecomendacionesActivity : AppCompatActivity() {

    private lateinit var binding : ActivityRecomendacionesBinding
    private var establecimientos = mutableListOf<Establecimiento>()
    private lateinit var geocoder: Geocoder
    private lateinit var idPlan: String
    private lateinit var pantalla: String
    private var isFabOpen=false
    private var rotation=false
    private lateinit var idGrupo : String
    private var latMia:Double= 4.0
    private var longMia:Double= 72.0

    private val keysUbicaciones: List<String> = listOf(
        "commercial.food_and_drink",
        "commercial.shopping_mall",
        "catering.restaurant",
        "commercial.books",
        "entertainment",
        "commercial.hobby",
        "commercial.clothing",
        "commercial.art",
    )

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding= ActivityRecomendacionesBinding.inflate(layoutInflater)
        geocoder = Geocoder(baseContext)
        //binding representa toda la actividad
        setContentView(binding.root)
        idGrupo=intent.getStringExtra("idGrupo").toString()
        idPlan= intent.getStringExtra("idPlan").toString()
        Log.i(TAG, "ENCONTRADO JEJE - $idPlan")

        pantalla= intent.getStringExtra("pantalla").toString()
        latMia=intent.getDoubleExtra("latitud",0.0)
        longMia= intent.getDoubleExtra("longitud",0.0)

        Log.i("mi ubicacion","$latMia,$longMia")
        inicializarSpinner()
        inicializarSeleccionLista()
        inicializarBotones()
    }




    private fun inicializarSeleccionLista() {
        //aqui lo que se seleccione se guardara y enviara a seleccionar ubicacion para que se muestre
        //OJO
        binding.listView.setOnItemClickListener { parent, view, position, id ->

            //val pantalla=intent.getStringExtra("pantalla")
            val selectedLugar = establecimientos[position] // Obtiene el objeto Pais seleccionado

            var intent: Intent
            intent = Intent(baseContext, ElegirUbicacionActivity::class.java)
            Log.i("seleccion","${selectedLugar.getLatitude()},${selectedLugar.getLongitude()}")
            intent.putExtra("pantalla",pantalla)
            intent.putExtra("longitud",selectedLugar.getLongitude())
            intent.putExtra("latitud",selectedLugar.getLatitude())
            intent.putExtra("idPlan", idPlan)
            intent.putExtra("idGrupo", idGrupo)
            intent.putExtra("recomendacion","recomendacion")
            Log.i(TAG, "rInfo envia - Longitud: ${selectedLugar.getLongitude()}, Latitud: ${selectedLugar.getLatitude()}")
            // Pasa el objeto Pais como un extra del Intent
            startActivity(intent)
        }
    }
    private fun inicializarSpinner() {
        val spinner: Spinner = findViewById(R.id.ubicaciones)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val seleccion = spinner.selectedItem as String
                llenarLista(keysUbicaciones[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun llenarLista(seleccion: String)
    {
        crearConsulta(seleccion)

    }

    private fun crearConsulta(seleccion: String) {
        val queue: RequestQueue = Volley.newRequestQueue(this)
        val token = "f2336d652f6644439f3fe49fd7fa1251"
        val url="https://api.geoapify.com/v2/places?categories=$seleccion&filter=circle:$longMia,$latMia,6000&bias=proximity:$longMia,$latMia&lang=en&limit=40&apiKey=$token"
        val imagen="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRNiROUXTw6BUWAP9A08C-1vcvI_YNWF4KzYtzTRAb9LQ&s"
        // Solicitud GET de Volley.
        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,

            Response.Listener<JSONObject> { response ->

                try {
                    establecimientos.clear()
                    Log.i("resultado", "$response")

                    val features = response.getJSONArray("features")
                    for (i in 0 until features.length()) {
                        // Acceder a "names" si existe.
                        val feature = features.getJSONObject(i)
                        val properties = feature.getJSONObject("properties")

                        if(properties.has("name")
                            && properties.has("street")
                            && properties.has("lat")
                            && properties.has("lon"))
                        {
                            var name = properties.getString("name")
                            var latitude = properties.getDouble("lat")
                            var longitude = properties.getDouble("lon")
                            var street = properties.getString("street")

                            var establecimiento=Establecimiento(name, imagen, latitude, longitude, street)
                            // Hacer lo que necesites con los datos obtenidos.
                            Log.i("resultado", "$name,  $latitude, $longitude, $street")

                            establecimientos.add (establecimiento)
                        }

                    }

                    Log.d("crearConsulta", "Tamaño de la lista 2 de establecimientos: ${establecimientos.size}")

                    var adapter = AdapterEstablecimiento(applicationContext, establecimientos)

                    // Asignar el adaptador al ListView
                    binding.listView.adapter = adapter
                    // Procesa el JSON según tus necesidades.
                    // Aquí puedes extraer y manejar los datos como desees.
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                // Manejo de errores de la solicitud.
                Log.e("Error en la solicitud", "Error en la solicitud: " + error.message)
            }) {
        }

        // Agrega la solicitud a la cola de solicitudes.
        queue.add(jsonObjectRequest)
    }

    private fun inicializarBotones() {
        val usuario: UsuarioAmigo = UsuarioAmigo()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    startActivity(Intent(baseContext, VerGruposActivity::class.java))

                    true
                }
                R.id.cuenta_bar -> {
                    // Respond to navigation item 2 click
                    var intent = Intent(baseContext, PerfilConfActivity::class.java)
                    intent.putExtra("user", usuario)
                    startActivity(intent)
                    true
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
        val fechaActual = Date()

        return when {
            fechaActual.before(dateInicio) -> "Activo"
            fechaActual.after(dateFinal) -> "Cerrado"
            else -> "Abierto"
        }
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