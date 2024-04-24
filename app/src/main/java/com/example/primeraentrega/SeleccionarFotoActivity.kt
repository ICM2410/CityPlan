package com.example.primeraentrega

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.primeraentrega.databinding.ActivitySeleccionarFotoBinding
import java.io.File

class SeleccionarFotoActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySeleccionarFotoBinding


    val getContentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback {
            loadImage(it)
        })

    val getContentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback {
            if(it){
                loadImage(uriCamera)
            }
        })

    private lateinit var uriCamera : Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeleccionarFotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val file = File(getFilesDir(), "picFromCamera");
        uriCamera = FileProvider.getUriForFile(baseContext,baseContext.packageName + ".fileprovider", file)

        binding.buttonGallery.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        binding.buttonCamera.setOnClickListener {
            getContentCamera.launch(uriCamera)
        }

    }

    private fun loadImage(uri : Uri?) {
        val returnIntent = Intent()
        returnIntent.putExtra("imageUri", uri.toString())
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

}