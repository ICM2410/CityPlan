package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityEditarGrupoBinding
import com.example.primeraentrega.databinding.ActivityPlanesBinding

class PlanesActivity : AppCompatActivity() {
    private lateinit var binding : ActivityPlanesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPlanesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inicializarBotones()
    }
    private fun inicializarBotones() {
        binding.botonPlanActivo.setOnClickListener {
            startActivity(Intent(baseContext, PlanActivity::class.java))
        }

        binding.botonPlanInactivo.setOnClickListener {
            startActivity(Intent(baseContext, PlanFinalizadoActivity::class.java))
        }

        binding.botonAgregarPlan.setOnClickListener {
            startActivity(Intent(baseContext, CrearPlanActivity::class.java))
        }

    }
}