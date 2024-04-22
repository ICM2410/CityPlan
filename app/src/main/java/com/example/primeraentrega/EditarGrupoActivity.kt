package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityAgregarContactosBinding
import com.example.primeraentrega.databinding.ActivityChatBinding
import com.example.primeraentrega.databinding.ActivityEditarGrupoBinding

class EditarGrupoActivity : AppCompatActivity() {

    private lateinit var binding : ActivityEditarGrupoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityEditarGrupoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inicializarBotones()
    }
    private fun inicializarBotones() {
        binding.buttonAgregarMiembros.setOnClickListener {
            startActivity(Intent(baseContext, AgregarContactosActivity::class.java))
        }

        binding.buttonSalir.setOnClickListener {
            startActivity(Intent(baseContext, VerGruposActivity::class.java))
        }

        binding.buttonGuardar.setOnClickListener {
            startActivity(Intent(baseContext, ChatActivity::class.java))
        }

        binding.botonConfiguracion.setOnClickListener {
            startActivity(Intent(baseContext, ConfiguracionActivity::class.java))
        }

        binding.botonHome.setOnClickListener {
            startActivity(Intent(baseContext, VerGruposActivity::class.java))
        }
    }
}