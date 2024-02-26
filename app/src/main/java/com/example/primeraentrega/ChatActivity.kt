package com.example.primeraentrega


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.primeraentrega.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    private lateinit var binding : ActivityChatBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        inicializarBotones()
    }

    private fun inicializarBotones() {
        binding.botonVerPlanes.setOnClickListener {
            startActivity(Intent(baseContext, PlanesActivity::class.java))
        }

        binding.configGrupo.setOnClickListener {
            startActivity(Intent(baseContext, EditarGrupoActivity::class.java))
        }
    }
}