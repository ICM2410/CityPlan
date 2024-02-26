package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityCrearPlanBinding
import com.example.primeraentrega.databinding.ActivityPlanBinding

class CrearPlanActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCrearPlanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityCrearPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inicializarBotones()
    }

    private fun inicializarBotones() {
        binding.crearplanButton.setOnClickListener {
            startActivity(Intent(baseContext, PlanActivity::class.java))
        }

        binding.seleccionarUbicacion.setOnClickListener {
            startActivity(Intent(baseContext, ElegirUbicacionActivity::class.java))
        }

    }
}