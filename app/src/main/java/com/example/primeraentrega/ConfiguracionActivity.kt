package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityConfiguracionBinding
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding

class ConfiguracionActivity : AppCompatActivity() {
    private lateinit var binding : ActivityConfiguracionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarBotones()
    }

    private fun inicializarBotones()
    {
        binding.perfilconftext.setOnClickListener {
            startActivity(Intent(baseContext,PerfilConfActivity::class.java))
        }

        binding.huellaconftext.setOnClickListener {
            startActivity(Intent(baseContext,ConfigurarHuellaActivity::class.java))
        }

        binding.permisosconftext.setOnClickListener {
            startActivity(Intent(baseContext,PermisosActivity::class.java))
        }


    }
}