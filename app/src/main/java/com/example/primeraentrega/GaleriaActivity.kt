package com.example.primeraentrega

import PhotoGalleryAdapter

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.primeraentrega.databinding.ActivityGaleriaBinding
import java.io.File


class GaleriaActivity : AppCompatActivity() {

    private lateinit var binding : ActivityGaleriaBinding
    private lateinit var uriCamera : Uri
    private val REQUEST_CAMERA_PERMISSION = 100
    private val photoList = mutableListOf<Uri>()
    private lateinit var photoGalleryAdapter: PhotoGalleryAdapter

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityGaleriaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val file = File(getFilesDir(), "picFromCamera");
        uriCamera = FileProvider.getUriForFile(baseContext,baseContext.packageName + ".fileprovider", file)

        // Configurar RecyclerView
        binding.photoGalleryRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        photoGalleryAdapter = PhotoGalleryAdapter(photoList)
        binding.photoGalleryRecyclerView.adapter = photoGalleryAdapter


        binding.buttonSeleccionarFoto.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        /*
        binding.buttonTomarFoto.setOnClickListener {
            // Permiso de cámara concedido, lanzar la actividad de la cámara
            getContentCamera.launch(uriCamera)
        }
        */

    }

    private fun loadImage(uri : Uri?) {
        // Agregar la Uri recibida a la lista de fotos
        if (uri != null) {
            photoList.add(uri)
        }
        // Notificar al adaptador que se ha agregado una nueva foto
        photoGalleryAdapter.notifyItemInserted(photoList.size - 1)
        // Crear una nueva Uri para la próxima foto
        val file = File(getFilesDir(), "picFromCamera")
        uriCamera = FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)
    }




}