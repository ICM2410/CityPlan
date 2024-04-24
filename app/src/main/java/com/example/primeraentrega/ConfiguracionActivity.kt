package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.primeraentrega.databinding.ActivityConfiguracionBinding
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding
import com.example.primeraentrega.usuario.usuario

class ConfiguracionActivity : AppCompatActivity() {
    private lateinit var binding : ActivityConfiguracionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val usuario = intent.getSerializableExtra("user") as? usuario

        inicializarBotones(usuario)
    }

    private fun inicializarBotones(usuario: usuario?)
    {
        binding.perfilconftext.setOnClickListener {
            startActivity(Intent(baseContext,PerfilConfActivity::class.java))
        }

        binding.huellaconftext.setOnClickListener {

            var intent=Intent(baseContext,ConfigurarHuellaActivity::class.java)
            intent.putExtra("user", usuario)

            startActivity(intent)

        }

        binding.permisosconftext.setOnClickListener {
            startActivity(Intent(baseContext,PermisosActivity::class.java))
        }


    }
}