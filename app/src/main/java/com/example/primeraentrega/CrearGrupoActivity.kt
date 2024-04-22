package com.example.primeraentrega

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.primeraentrega.databinding.ActivityCrearGrupoBinding
import com.example.primeraentrega.databinding.ActivityVerGruposBinding

class CrearGrupoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearGrupoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearGrupoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarBotones()
    }

    private val SELECCIONAR_FOTO_REQUEST_CODE = 1

    private fun inicializarBotones() {

        binding.ButtonSeleccionarFoto.setOnClickListener {
            val intent = Intent(this@CrearGrupoActivity, SeleccionarFotoActivity::class.java)
            startActivityForResult(intent, SELECCIONAR_FOTO_REQUEST_CODE)
        }


        binding.buttonAgregarMiembros.setOnClickListener {
            startActivity(Intent(baseContext, AgregarContactosActivity::class.java))
        }

        binding.buttonGuardar.setOnClickListener {
            startActivity(Intent(baseContext, ChatActivity::class.java))
        }

        binding.botonHome.setOnClickListener {
            startActivity(Intent(baseContext, VerGruposActivity::class.java))
        }

        binding.botonConfiguracion.setOnClickListener {
            startActivity(Intent(baseContext, ConfiguracionActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECCIONAR_FOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.getStringExtra("imageUri")
            if (imageUri != null) {
                // Cargar la imagen en tu botón o ImageView
                Glide.with(this).load(Uri.parse(imageUri)).into(binding.ButtonSeleccionarFoto)
            }
        }
    }

}