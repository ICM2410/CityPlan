package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.databinding.ActivityConfiguracionBinding

class ConfiguracionActivity : AppCompatActivity() {
    private lateinit var binding : ActivityConfiguracionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val usuario = intent.getSerializableExtra("user") as? UsuarioAmigo

        inicializarBotones(usuario)
    }

    private fun inicializarBotones(usuario: UsuarioAmigo?)
    {
        binding.perfilconftext.setOnClickListener {
            startActivity(Intent(baseContext,PerfilConfActivity::class.java))
        }

        binding.huellaconftext.setOnClickListener {

            var intent=Intent(baseContext,ConfigurarHuellaActivity::class.java)
            intent.putExtra("user", usuario)

            startActivity(intent)

        }


    }
}