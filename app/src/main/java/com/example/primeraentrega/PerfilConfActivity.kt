package com.example.primeraentrega

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.databinding.ActivityPerfilConfBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.security.MessageDigest

class PerfilConfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilConfBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userId: String
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

        // Obtener el userId del Intent
        userId = intent.getStringExtra("userId").toString()

        //val usuario = intent.getSerializableExtra("user") as? Usuario
        binding.bottomNavigation.selectedItemId = R.id.cuenta_bar

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        inicializarBotones()
    }

    private val SELECCIONAR_FOTO_REQUEST_CODE = 1

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
                cambiarContraseña(nuevaContraseña)
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
        // Referencia al almacenamiento de Firebase
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${userId}/profile.jpg")

        // Subir la imagen al Firebase Storage
        val uploadTask = imageRef.putFile(uri)

        // Manejar el éxito o el fracaso de la carga de la imagen
        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // La imagen se ha subido correctamente, obtener la URL de la imagen
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Guardar la URL de la imagen en la base de datos
                    guardarUrlImagenEnFirebaseDatabase(downloadUri.toString())
                }
            } else {
                // La carga de la imagen falló
                // Manejar el error si es necesario
            }
        }
    }

    private fun guardarUrlImagenEnFirebaseDatabase(imageUrl: String) {
        // Referencia al nodo del usuario en la base de datos
        val usuarioRef = database.getReference("usuarios").child(userId)

        // Guardar la URL de la imagen en la base de datos
        usuarioRef.child("imageUrl").setValue(imageUrl)
            .addOnSuccessListener {
                // La URL de la imagen se ha guardado correctamente en la base de datos
                // Realizar cualquier otra acción necesaria, como mostrar un mensaje de éxito
            }
            .addOnFailureListener { e ->
                // La URL de la imagen no se pudo guardar en la base de datos
                // Manejar el error si es necesario
            }
    }

    private fun solicitarHuella(usuario: UsuarioAmigo?) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Aquí puedes generar un ID único basado en los datos biométricos
                    val biometricData = result.cryptoObject?.cipher?.iv ?: ByteArray(0)
                    val biometricId = generateBiometricId(biometricData)

                    // Asignar el ID de la huella dactilar al usuario
                    usuario?.huella = biometricId


                    // Guardar el usuario actualizado en Firebase
                    guardarUsuarioEnFirebase(usuario)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación de huella dactilar")
            .setSubtitle("Toque el sensor de huella dactilar")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Función para generar un ID único basado en los datos biométricos
    private fun generateBiometricId(biometricData: ByteArray): String {
        // Crear una instancia del algoritmo de hash SHA-256
        val digest = MessageDigest.getInstance("SHA-256")

        // Calcular el hash de los datos biométricos
        val hashBytes = digest.digest(biometricData)

        // Convertir el hash en una cadena hexadecimal
        val hexString = StringBuilder()
        for (byte in hashBytes) {
            // Convertir cada byte a su representación hexadecimal y agregarlo a la cadena
            hexString.append(String.format("%02x", byte))
        }

        // Devolver el ID único generado
        return hexString.toString()
    }

    private fun guardarUsuarioEnFirebase(usuario: UsuarioAmigo?) {

    }

    private fun loadImage(uri: Uri?) {
        val imageStream = contentResolver.openInputStream(uri!!)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.imageViewImagen.setImageBitmap(bitmap)

    }

    private fun guardarPerfil() {
        // Obtener la URI de la imagen seleccionada
        val uriImagen = obtenerUriImagenSeleccionada()

        // Obtener el nuevo nombre de usuario y descripción del usuario
        val nuevoNombreUsuario = binding.user.text.toString()
        val nuevaDescripcionUsuario = binding.telephone.text.toString()

        // Actualizar el nombre de usuario y la descripción del usuario en Firebase Realtime Database
        val usuarioRef = database.getReference("Usuario").child(userId)
        usuarioRef.child("username").setValue(nuevoNombreUsuario)
        usuarioRef.child("telefono").setValue(nuevaDescripcionUsuario)

        // Guardar la imagen en Firebase Storage y la URL en Firebase Realtime Database
        if (uriImagen != null) {
            guardarImagenEnFirebaseStorage(uriImagen)
        } else {
            // Manejar el caso de que no se haya seleccionado ninguna imagen
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

}
