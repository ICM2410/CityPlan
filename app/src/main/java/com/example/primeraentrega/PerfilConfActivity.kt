package com.example.primeraentrega

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.databinding.ActivityPerfilConfBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.security.MessageDigest

class PerfilConfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilConfBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    lateinit var uriCamera: Uri

    val getContentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback {
            loadImage(it!!)
        })

    val getContentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(), ActivityResultCallback {
            if (it) {
                loadImage(uriCamera)
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilConfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()


        binding.bottomNavigation.selectedItemId = R.id.cuenta_bar

        // Cargar la imagen del usuario desde Firebase Storage
        cargarImagenUsuarioDesdeFirebaseStorage()

        cargarInfoUsuario()

        inicializarBotones()
    }

    private fun cargarInfoUsuario() {
        auth.currentUser?.let { database.getReference("Usuario").child(it.uid) }
            ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Crea un objeto PosAmigo con la información del usuario
                    val usuario = dataSnapshot.getValue(UsuarioAmigo::class.java)

                    if (usuario != null) {
                        binding.user.setText(usuario.username)
                        binding.telephone.setText(usuario.telefono.toString())
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    // Maneja el error en caso de que ocurra
                    println("Error al obtener los datos del usuario: ${databaseError.message}")
                }
            })
    }
    private fun inicializarBotones() {

        val file = File(getFilesDir(), "picFromCamera");
        uriCamera = FileProvider.getUriForFile(
            baseContext,
            baseContext.packageName + ".fileprovider",
            file
        )

        binding.buttonGaleria.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        binding.buttonCamara.setOnClickListener {
            getContentCamera.launch(uriCamera)
        }

        binding.guardarperfil.setOnClickListener {
            // Obtener la nueva contraseña del campo de entrada
            val nuevaContraseña = binding.password.text.toString()

            if(!nuevaContraseña.isEmpty())
            {
                cambiarContraseña(nuevaContraseña)
            }
            // Guardar el perfil, incluida la imagen si se selecciona una
            guardarPerfil()
        }


        val usuario: UsuarioAmigo = UsuarioAmigo()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    startActivity(Intent(baseContext, VerGruposActivity::class.java))
                    true
                }
                R.id.cuenta_bar -> {
                    // Respond to navigation item 2 click
                    true
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
    }

    private fun obtenerUriImagenSeleccionada(): Uri? {
        // Crear un intent para seleccionar una imagen de la galería
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"

        // Comprobar si hay alguna actividad que pueda manejar este intent
        if (intent.resolveActivity(packageManager) != null) {
            // Lanzar el intent para seleccionar una imagen de la galería
            getContentGallery.launch(intent.toString())
        } else {
            // No hay ninguna actividad que pueda manejar este intent
            // Manejar el caso de error si es necesario
        }

        // La URI de la imagen seleccionada se obtendrá en el callback de getContentGallery
        return null
    }


    private fun guardarImagenEnFirebaseStorage(uri: Uri) {
        // Obtener el UID del usuario actual
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Verificar que el UID del usuario no sea nulo
        if (userId != null) {
            // Referencia al almacenamiento de Firebase
            val storageRef = FirebaseStorage.getInstance().reference
            // Nombre de la imagen en Firebase Storage (sin la extensión)
            val imageName = "usuarios/$userId.jpg"

            // Subir la imagen al Firebase Storage
            val uploadTask = storageRef.child(imageName).putFile(uri)

            // Manejar el éxito o el fracaso de la carga de la imagen
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // La imagen se ha subido correctamente
                    // Guardar la URL de la imagen en Firebase Realtime Database
                    val imageUrl = imageName // La URL de la imagen es la misma que el nombre de la imagen
                    guardarUrlImagenEnFirebaseDatabase(imageUrl)
                } else {
                    // La carga de la imagen falló
                    // Manejar el error si es necesario
                    Toast.makeText(this, "Error al cargar la imagen: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // El UID del usuario es nulo
            // Manejar el caso si el usuario no ha iniciado sesión
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }


    private fun guardarUrlImagenEnFirebaseDatabase(imageUrl: String) {
        // Referencia al nodo del usuario en la base de datos
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if(userId!=null)
        {
            val usuarioRef = database.getReference("Usuario").child(userId)

            // Guardar la URL de la imagen en la base de datos
            usuarioRef.child("imagen").setValue(imageUrl)
                .addOnSuccessListener {
                    // La URL de la imagen se ha guardado correctamente en la base de datos
                    // Realizar cualquier otra acción necesaria, como mostrar un mensaje de éxito
                    Toast.makeText(this, "Imagen guardada con éxito", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // La URL de la imagen no se pudo guardar en la base de datos
                    // Manejar el error si es necesario
                    Toast.makeText(this, "Error al guardar la URL de la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun loadImage(uri: Uri?) {
        val imageStream = contentResolver.openInputStream(uri!!)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.imageViewImagen.setImageBitmap(bitmap)

        // Set the tag of the image view with the URI of the selected image
        binding.imageViewImagen.tag = uri
    }


    private fun guardarPerfil() {
        // Obtener el nuevo nombre de usuario y descripción del usuario
        val nuevoNombreUsuario = binding.user.text.toString()
        val nuevaDescripcionUsuario = binding.telephone.text.toString().toInt()

        // Actualizar el nombre de usuario y la descripción del usuario en Firebase Realtime Database
        val usuarioRef = auth.currentUser?.uid?.let { database.getReference("Usuario").child(it) }
        if (usuarioRef != null) {
            usuarioRef.child("username").setValue(nuevoNombreUsuario)
            usuarioRef.child("telefono").setValue(nuevaDescripcionUsuario)
        }
        // Obtener la URI de la imagen seleccionada
        val uriImagen = binding.imageViewImagen.tag as? Uri

        // Guardar la imagen en Firebase Storage y la URL en Firebase Realtime Database
        if (uriImagen != null) {
            guardarImagenEnFirebaseStorage(uriImagen)
        }

    }


    private fun cambiarContraseña(nuevaContraseña: String) {
        val user = FirebaseAuth.getInstance().currentUser

        user?.updatePassword(nuevaContraseña)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Contraseña actualizada exitosamente
                    Toast.makeText(this, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    // Error al actualizar la contraseña
                    Toast.makeText(this, "Error al actualizar la contraseña: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun cargarImagenUsuarioDesdeFirebaseStorage() {
        // Referencia al almacenamiento de Firebase
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("usuarios/${auth.currentUser?.uid}.jpg")

        // Descargar la URL de la imagen del Firebase Storage
        imageRef.downloadUrl
            .addOnSuccessListener { downloadUri ->
                // Utilizar una biblioteca de carga de imágenes para cargar la imagen desde la URL
                Glide.with(this)
                    .load(downloadUri)
                    .into(binding.imageViewImagen)
            }
            .addOnFailureListener { e ->
                // Manejar el caso de error si no se puede obtener la URL de la imagen
                Toast.makeText(this, "Error al cargar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
