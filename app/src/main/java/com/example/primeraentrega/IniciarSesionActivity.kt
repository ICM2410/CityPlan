package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding

import com.example.primeraentrega.usuario.usuario
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

class IniciarSesionActivity : AppCompatActivity() {

    private lateinit var binding : ActivityIniciarSesionBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef:DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityIniciarSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        inicializarBotones()
    }

    private fun inicializarBotones()
    {
        binding.buttonIniciarSesion.setOnClickListener {

            val user = binding.user.text.toString()
            val password = binding.password.text.toString()

            

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