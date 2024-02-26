package com.example.primeraentrega

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.primeraentrega.Clases.Establecimiento
import com.example.primeraentrega.Adapters.AdapterEstablecimiento
import com.example.primeraentrega.databinding.ActivityRecomendacionesBinding
import org.json.JSONObject


class RecomendacionesActivity : AppCompatActivity() {

    private lateinit var binding : ActivityRecomendacionesBinding
    private var establecimientos = mutableListOf<Establecimiento>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding= ActivityRecomendacionesBinding.inflate(layoutInflater)

        //binding representa toda la actividad
        setContentView(binding.root)

        llenarLista()
    }

    private fun llenarLista()
    {

        val json_string = this.assets.open("establecimientos.json").bufferedReader().use{
            it.readText()
        }

        var json = JSONObject(json_string);
        var establecimientosJsonArray = json.getJSONArray("establecimientos");


        for (i in 0..establecimientosJsonArray.length()-1) {

            var jsonObject = establecimientosJsonArray.getJSONObject(i)
            var nombre = jsonObject.getString("Nombre")
            var imagen= jsonObject.getString("imagen")

            //Crear el objeto pais y agregarlo al arreglo
            var establecimiento = Establecimiento(nombre, imagen)
            establecimientos.add(establecimiento)
        }


        // val adapter = ArrayAdapter(applicationContext, R.layout.simple_list_item_1, paises.map { it.getNombre()})

        val adapter = AdapterEstablecimiento(applicationContext, establecimientos)

                // Asignar el adaptador al ListView
        binding.listView.adapter = adapter
    }

}