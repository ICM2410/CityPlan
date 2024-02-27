package com.example.primeraentrega

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.primeraentrega.databinding.ActivityGaleriaBinding


class GaleriaActivity : AppCompatActivity() {

    private lateinit var binding : ActivityGaleriaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityGaleriaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tomarFoto.setOnClickListener {
            takePicture()
        }
    }

    private val CAMERA_REQUEST = 1888 // Constante para identificar la solicitud de la cámara

    private fun takePicture() {
        // Intent para abrir la aplicación de la cámara
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            // Iniciar la actividad de la cámara y esperar el resultado
            startActivityForResult(takePictureIntent, CAMERA_REQUEST)
        } catch (e: ActivityNotFoundException) {
            // Si no se encuentra ninguna actividad para manejar la acción de la cámara
            Log.e("PERMISSION_APP", "No se pudo abrir la aplicación de la cámara: " + e.message)
        }
    }


}