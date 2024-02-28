package com.example.primeraentrega

import android.content.ActivityNotFoundException
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.primeraentrega.databinding.ActivityGaleriaBinding
import java.io.File


class GaleriaActivity : AppCompatActivity() {

    private lateinit var binding : ActivityGaleriaBinding
    private lateinit var uriCamera : Uri
    private val REQUEST_CAMERA_PERMISSION = 100

    val getContentCamera = registerForActivityResult(ActivityResultContracts.TakePicture(),
        ActivityResultCallback {
            if(it){
                loadImage(uriCamera)
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityGaleriaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val file = File(getFilesDir(), "picFromCamera");
        uriCamera = FileProvider.getUriForFile(baseContext,baseContext.packageName + ".fileprovider", file)

        binding.tomarFoto.setOnClickListener {
            takePicture()
        }
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            getContentCamera.launch(uriCamera)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun loadImage(uri : Uri?) {
        val imageStream = getContentResolver().openInputStream(uri!!)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.imageView5.setImageBitmap(bitmap)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso de cámara concedido, puedes lanzar la actividad de la cámara
                getContentCamera.launch(uriCamera)
            } else {
                // Permiso de cámara denegado, puedes mostrar un mensaje al usuario o tomar alguna acción adicional
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

}