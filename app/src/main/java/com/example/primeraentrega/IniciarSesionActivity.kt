package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding

class IniciarSesionActivity : AppCompatActivity() {

    private lateinit var binding : ActivityIniciarSesionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityIniciarSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarBotones()
    }

    private fun inicializarBotones()
    {
        binding.buttonIniciarSesion.setOnClickListener {
            startActivity(Intent(baseContext,VerGruposActivity::class.java))
        }

        binding.buttonHuella.setOnClickListener {
            startActivity(Intent(baseContext,IniciarSesionHuellaActivity::class.java))
        }

        binding.buttonRegistrarse.setOnClickListener {
            startActivity(Intent(baseContext,RegistrarUsuarioActivity::class.java))
        }


    }
}