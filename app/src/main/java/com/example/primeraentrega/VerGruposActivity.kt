package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityCrearGrupoBinding
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding
import com.example.primeraentrega.databinding.ActivityVerGruposBinding

class VerGruposActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerGruposBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerGruposBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarBotones()
    }

    private fun inicializarBotones() {
        binding.grupoChocmelos.setOnClickListener {
            startActivity(Intent(baseContext, ChatActivity::class.java))
        }

        binding.botonConfiguracion.setOnClickListener {
            startActivity(Intent(baseContext, ConfiguracionActivity::class.java))
        }

        binding.botonAgregarGrupo.setOnClickListener {
            startActivity(Intent(baseContext, CrearGrupoActivity::class.java))
        }

    }
}