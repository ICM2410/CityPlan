package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding
import com.example.primeraentrega.databinding.ActivityRegistrarUsuarioBinding

class RegistrarUsuarioActivity : AppCompatActivity() {

    private lateinit var binding : ActivityRegistrarUsuarioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityRegistrarUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.guardarperfil.setOnClickListener {
            startActivity(Intent(baseContext,IniciarSesionActivity::class.java))
        }
    }
}