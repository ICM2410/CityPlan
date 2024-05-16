package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.databinding.ActivityEditarGrupoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.Date

class EditarGrupoActivity : AppCompatActivity() {

    private lateinit var binding : ActivityEditarGrupoBinding
    private lateinit var idGrupo : String
    private lateinit var userId : String
    private var idPlan : String=""
    lateinit var uriCamera : Uri

    val getContentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback {
            loadImage(it!!)
        })

    val getContentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),ActivityResultCallback {
            if(it){
                loadImage(uriCamera)
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityEditarGrupoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        idGrupo=intent.getStringExtra("idGrupo").toString()
        userId = intent.getStringExtra("userId").toString()
        inicializarBotones()
    }

    private val SELECCIONAR_FOTO_REQUEST_CODE = 1

    private var isFabOpen=false
    private var rotation=false
    private fun inicializarBotones() {


        val file = File(getFilesDir(), "picFromCamera");
        uriCamera =  FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)

        binding.botonGaleria1.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        binding.botonCamara1.setOnClickListener {
            getContentCamera.launch(uriCamera)
        }


        binding.buttonSalir.setOnClickListener {
            // Verificar si el ID del usuario no es nulo
            if (userId != null) {
                // Obtener la referencia del grupo en la base de datos
                val grupoRef = FirebaseDatabase.getInstance().getReference("Groups").child(idGrupo)

                // Eliminar al usuario de la lista de miembros del grupo
                grupoRef.child("integrantes").child(userId).removeValue()
                    .addOnSuccessListener {
                        // El usuario se eliminó correctamente del grupo
                        Toast.makeText(applicationContext, "Saliste del grupo exitosamente", Toast.LENGTH_SHORT).show()

                        // Redirigir al usuario a la actividad anterior o a la actividad principal
                        finish() // Finalizar la actividad actual y regresar a la actividad anterior
                    }
                    .addOnFailureListener { e ->
                        // Error al eliminar al usuario del grupo
                        Toast.makeText(applicationContext, "Error al salir del grupo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // El ID del usuario es nulo
                Toast.makeText(applicationContext, "ID del usuario nulo", Toast.LENGTH_SHORT).show()
            }
            val intent = Intent(this, VerGruposActivity::class.java)
            startActivity(intent)
        }



        binding.buttonGuardar.setOnClickListener {
            val nombreGrupo = binding.editTextNombreGrupo.text.toString()
            val descripcionGrupo = binding.editTextDescGrupo.text.toString()
            if (validateForm(nombreGrupo, descripcionGrupo)) {
                actualizarDatosGrupo(nombreGrupo, descripcionGrupo)
            }
        }

        binding.fotoSeleccionada1.setOnClickListener {
            val intent = Intent(this@EditarGrupoActivity, SeleccionarFotoActivity::class.java)
            startActivityForResult(intent, SELECCIONAR_FOTO_REQUEST_CODE)
        }

        val usuario: UsuarioAmigo = UsuarioAmigo()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    startActivity(Intent(baseContext, VerGruposActivity::class.java))

                    true
                }
                R.id.cuenta_bar -> {
                    val executor = ContextCompat.getMainExecutor(this)
                    val biometricPrompt = BiometricPrompt(this, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                // Aquí puedes realizar alguna acción después de la autenticación exitosa
                                // Por ejemplo, mostrar un mensaje o iniciar una nueva actividad
                                var intent = Intent(baseContext, PerfilConfActivity::class.java)
                                intent.putExtra("user", usuario)
                                startActivity(intent)
                                //startActivity(Intent(baseContext, PerfilConfActivity::class.java))
                                //startActivity(Intent(baseContext, VerGruposActivity::class.java))
                                true
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Autenticación de huella dactilar")
                        .setSubtitle("Toque el sensor de huella dactilar")
                        .setNegativeButtonText("Cancelar")
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                    // Respond to navigation item 2 click
                    false
                }
                R.id.salir_bar -> {
                    // Respond to navigation item 3 click
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                    true
                }
                else -> false
            }
        }

        initShowout(binding.pasadosView)
        initShowout(binding.nuevoView)
        initShowout(binding.activoView)
        initShowout(binding.planesView)
        binding.fabMenuPlan.setOnClickListener {
            if(!isFabOpen)
            {
                showFabMenu();
            }
            else
            {
                closeFabMenu();
            }
        }

        fabClicks()
    }

    private fun validateForm(nombreGrupo: String, descripcionGrupo: String): Boolean {
        var valid = false
        if (nombreGrupo.isEmpty()) {
            Toast.makeText(applicationContext, "¡El nombre del grupo es obligatorio!", Toast.LENGTH_SHORT).show()
        } else if (descripcionGrupo.isEmpty()) {
            Toast.makeText(applicationContext, "¡La descripción del grupo es obligatoria!", Toast.LENGTH_SHORT).show()
        } else {
            valid = true
        }
        return valid
    }


    private fun fabClicks() {
        binding.fabPlanesPasados.setOnClickListener {
            var intent = Intent(baseContext, PlanesPasadosActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabCrearPlan.setOnClickListener {
            var intent = Intent(baseContext, CrearPlanActivity::class.java)
            intent.putExtra("pantalla", "planes")
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabMisPlanes.setOnClickListener {
            var intent = Intent(baseContext, PlanesActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabPlanActivo.setOnClickListener {
            revisarActivo()
        }
    }

    private fun revisarActivo() {
        var existe=false
        val ref = FirebaseDatabase.getInstance().getReference("Groups")
        ref.child(idGrupo).child("planes").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    // Obtiene los datos de cada usuario
                    val planId = userSnapshot.key // El ID del usuario
                    val planData = userSnapshot.getValue(Plan::class.java) // Los datos del usuario convertidos a objeto Usuario

                    // Aquí puedes realizar cualquier operación con los datos del usuario
                    println("ID de usuario: $planId")
                    println("Datos de usuario: $planData")

                    // Crea un objeto PosAmigo con la información del usuario
                    var status=""
                    val plan = planData?.let {
                        status=planAcrivo(planData.dateInicio,planData.dateFinal)
                    }

                    // Si el usuario y su ID no son nulos, añádelos al mapa integrantesMap
                    if (planId != null &&  plan != null && status!="Activo") {
                        existe=true
                        idPlan=planId
                    }
                }

                if(existe)
                {
                    var intent = Intent(baseContext, PlanActivity::class.java)
                    intent.putExtra("idGrupo", idGrupo)
                    intent.putExtra("idPlan", idPlan)
                    startActivity(intent)
                }
                else
                {
                    Toast.makeText(applicationContext, "No hay planes activos", Toast.LENGTH_LONG).show()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Maneja el error en caso de que ocurra
                println("Error al obtener los datos de planes: ${databaseError.message}")
            }
        })
    }

    private fun planAcrivo(dateInicio: java.util.Date, dateFinal: java.util.Date): String {
        val fechaActual = Date()

        return when {
            fechaActual.before(dateInicio) -> "Activo"
            fechaActual.after(dateFinal) -> "Cerrado"
            else -> "Abierto"
        }
    }


    private fun initShowout (v: View){
        v.apply {
            visibility = View. GONE
            translationY= height.toFloat()
            alpha = 0f
        }
    }

    private fun closeFabMenu() {
        rotation=rotateFAB()
        isFabOpen=false
        cerrar(binding.pasadosView)
        cerrar(binding.nuevoView)
        cerrar(binding.activoView)
        cerrar(binding.planesView)
    }

    private fun cerrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=1f
            translationY=0f
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        visibility= View.GONE
                    }
                })
                .alpha(0f)
                .start()
        }
    }

    private fun showFabMenu() {
        rotation=rotateFAB()
        isFabOpen=true

        //motrar info
        mostrar(binding.pasadosView)
        mostrar(binding.nuevoView)
        mostrar(binding.activoView)
        mostrar(binding.planesView)

    }

    private fun mostrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=0f
            translationY=height.toFloat()
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){})
                .alpha(1f)
                .start()
        }
    }

    private fun rotateFAB():Boolean {
        binding.fabMenuPlan.animate()
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter(){})
            .rotation(if(!isFabOpen) 180f else 0f)

        return isFabOpen
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECCIONAR_FOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.getStringExtra("imageUri")
            if (imageUri != null) {
                // Cargar la imagen en tu botón o ImageView y aplicar círculo de recorte
                Glide.with(this)
                    .load(Uri.parse(imageUri))
                    .circleCrop() // Aplicar círculo de recorte
                    .into(binding.fotoSeleccionada1)

            }
        }
    }




    private fun loadImage(uri : Uri?) {
        val imageStream = getContentResolver().openInputStream(uri!!)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.fotoSeleccionada1.setImageBitmap(bitmap)

        // Después de cargar la imagen, llamar al método para actualizar la foto del grupo
        updateGroupPhoto(uri)
    }

    private fun updateGroupPhoto(imageUri: Uri?) {
        val drawableFoto = binding.fotoSeleccionada1.drawable
        if (drawableFoto != null && imageUri != null) {
            if (drawableFoto is BitmapDrawable) {
                val bitmap = drawableFoto.bitmap
                val storageReference = FirebaseStorage.getInstance().getReference("Groups/$idGrupo")

                // Subir la imagen al almacenamiento de Firebase
                storageReference.putFile(imageUri).addOnSuccessListener { taskSnapshot ->
                    // Obtener la URL de la imagen subida
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        // Actualizar el campo 'fotoGrupo' en la base de datos con la ruta relativa de la imagen
                        FirebaseDatabase.getInstance().getReference("Groups").child(idGrupo)
                            .child("fotoGrupo").setValue("Groups/$idGrupo")
                            .addOnSuccessListener {
                                Toast.makeText(applicationContext, "Foto del grupo actualizada", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(applicationContext, "Error al actualizar la foto del grupo: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }.addOnFailureListener { e ->
                        Toast.makeText(applicationContext, "Error al obtener la URL de la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(applicationContext, "Error al subir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actualizarDatosGrupo(nombreGrupo: String, descripcionGrupo: String) {
        val gruposRef = FirebaseDatabase.getInstance().getReference("Groups")

        // Actualizar los datos del grupo en la base de datos
        gruposRef.child(idGrupo).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Verificar si el grupo existe en la base de datos
                if (snapshot.exists()) {
                    // Actualizar los datos del grupo
                    snapshot.ref.child("titulo").setValue(nombreGrupo)
                    snapshot.ref.child("descripcion").setValue(descripcionGrupo)
                        .addOnSuccessListener {
                            // Los datos del grupo se actualizaron correctamente
                            Toast.makeText(applicationContext, "Datos del grupo actualizados", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            // Error al actualizar los datos del grupo
                            Toast.makeText(applicationContext, "Error al actualizar los datos del grupo: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // El grupo no existe en la base de datos
                    Toast.makeText(applicationContext, "El grupo no existe en la base de datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Error al acceder a los datos del grupo
                Toast.makeText(applicationContext, "Error al acceder a los datos del grupo: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

