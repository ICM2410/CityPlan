package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.primeraentrega.databinding.ActivityCrearGrupoBinding
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding
import com.example.primeraentrega.databinding.ActivityVerGruposBinding
import com.example.primeraentrega.usuario.usuario

class VerGruposActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerGruposBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerGruposBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val usuario = intent.getSerializableExtra("user") as? usuario



        inicializarBotones(usuario)
        inicializarBotones()
    }

    private fun inicializarBotones(usuario: usuario?) {
        binding.grupoChocmelos.setOnClickListener {
            startActivity(Intent(baseContext, ChatActivity::class.java))
        }

        binding.botonConfiguracion.setOnClickListener {

            var intent = Intent(baseContext, ConfiguracionActivity::class.java)
            intent.putExtra("user", usuario)
            startActivity(intent)
        }

        binding.botonAgregarGrupo.setOnClickListener {
            startActivity(Intent(baseContext, CrearGrupoActivity::class.java))
        }
    }
}