package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityCrearPlanBinding
import com.example.primeraentrega.databinding.ActivityElegirUbicacionBinding

class ElegirUbicacionActivity : AppCompatActivity() {

    private lateinit var binding : ActivityElegirUbicacionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityElegirUbicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inicializarBotones()
    }

    private fun inicializarBotones() {
        binding.verRecomendacion.setOnClickListener {
            startActivity(Intent(baseContext, RecomendacionesActivity::class.java))
        }

        binding.guardar.setOnClickListener {
            startActivity(Intent(baseContext, CrearPlanActivity::class.java))
        }

    }
}