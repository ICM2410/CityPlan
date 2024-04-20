package com.example.primeraentrega

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.example.primeraentrega.Clases.Establecimiento
import com.example.primeraentrega.Adapters.AdapterEstablecimiento
import com.example.primeraentrega.databinding.ActivityRecomendacionesBinding
import org.json.JSONObject
import com.android.volley.toolbox.StringRequest
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONException


class RecomendacionesActivity : AppCompatActivity() {

    private lateinit var binding : ActivityRecomendacionesBinding
    private var establecimientos = mutableListOf<Establecimiento>()
    private lateinit var geocoder: Geocoder
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding= ActivityRecomendacionesBinding.inflate(layoutInflater)
        geocoder = Geocoder(baseContext)
        //binding representa toda la actividad
        setContentView(binding.root)

        inicializarSpinner()

        inicializarSeleccionLista()
    }


    private fun inicializarSeleccionLista() {
        //aqui lo que se seleccione se guardara y enviara a seleccionar ubicacion para que se muestre
        //OJO
        binding.listView.setOnItemClickListener { parent, view, position, id ->

            val pantalla=intent.getStringExtra("pantalla")
            val selectedLugar = establecimientos[position] // Obtiene el objeto Pais seleccionado

            var intent = Intent(baseContext, EditarPlanActivity::class.java)

            if(pantalla=="crear")
            {
                intent = Intent(baseContext, CrearPlanActivity::class.java)
            }

            intent.putExtra("longitud",selectedLugar.getLongitude())
            intent.putExtra("latitud",selectedLugar.getLatitude())
            intent.putExtra("pantalla","recomendacion")
            Log.i(TAG, "Info enviar - Longitud: ${selectedLugar.getLongitude()}, Latitud: ${selectedLugar.getLatitude()}")
            // Pasa el objeto Pais como un extra del Intent
            startActivity(intent)
        }
    }

    private var isFirstSelection = true
    private fun inicializarSpinner() {
        val spinner: Spinner = findViewById(R.id.ubicaciones)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val seleccion = spinner.selectedItem as String
                llenarLista(seleccion)
                if (!isFirstSelection) {
                    //val seleccion = spinner.selectedItem as String
                    //llenarLista(seleccion)
                } else {
                    isFirstSelection = false
                }
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
        val url = "https://travel-advisor.p.rapidapi.com/locations/v2/auto-complete"
        val query = "?query=$seleccion&lang=en_US&units=km"
        val fullUrl = "$url$query"
        val imagen="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRNiROUXTw6BUWAP9A08C-1vcvI_YNWF4KzYtzTRAb9LQ&s"

        // Solicitud GET de Volley.
        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, fullUrl, null,
            Response.Listener<JSONObject> { response ->
                // Manejo de la respuesta JSON.
                try {
                    establecimientos.clear()
                    val data = response.getJSONObject("data")
                    val typeaheadAutocomplete = data.getJSONObject("Typeahead_autocomplete")
                    val results = typeaheadAutocomplete.getJSONArray("results")

                    for (i in 0 until results.length()) {
                        val result = results.getJSONObject(i)

                        // Acceder a "names" si existe.
                        val namesObject = result.optJSONObject("detailsV2")?.optJSONObject("names")
                        val name = namesObject?.optString("name", "Nombre no disponible")

                        // Acceder a "geocode" si existe.
                        val geocodeObject = result.optJSONObject("detailsV2")?.optJSONObject("geocode")
                        val latitude = geocodeObject?.optDouble("latitude", 30000.0) ?: 0.0
                        val longitude = geocodeObject?.optDouble("longitude", 30000.0) ?: 0.0

                        // Hacer lo que necesites con los datos obtenidos.
                        println("Nombre: $name")
                        println("Latitud: $latitude")
                        println("Longitud: $longitude")

                        if (name != null) {
                            if( latitude!=30000.0 && name != "Nombre no disponible")
                            {
                                findAddress(LatLng(latitude, longitude))?.let {
                                    establecimientos.add (Establecimiento(name, imagen, latitude, longitude, it))
                                }
                            }
                        }
                    }

                    val adapter = AdapterEstablecimiento(applicationContext, establecimientos)

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
                Log.e(ContentValues.TAG, "Error en la solicitud: " + error.message)
            }) {

            // Override de la función getHeaders para agregar los encabezados necesarios.
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["X-RapidAPI-Key"] = "098e8444dbmsh2d59bc94f56c440p16b71bjsn13d0886ab7fc"
                headers["X-RapidAPI-Host"] = "travel-advisor.p.rapidapi.com"
                return headers
            }
        }

        // Agrega la solicitud a la cola de solicitudes.
        queue.add(jsonObjectRequest)
    }

    fun findAddress (location : LatLng):String?{
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 2)
        if(addresses != null && !addresses.isEmpty()){
            val addr = addresses.get(0)
            val locname = addr.getAddressLine(0)
            return locname
        }
        return null
    }

}