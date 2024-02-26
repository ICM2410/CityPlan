package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityCrearGrupoBinding
import com.example.primeraentrega.databinding.ActivityPerfilConfBinding

class PerfilConfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilConfBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilConfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarBotones()
    }

    private fun inicializarBotones() {
        binding.guardarperfil.setOnClickListener {
            startActivity(Intent(baseContext, ConfiguracionActivity::class.java))
        }


    }
}