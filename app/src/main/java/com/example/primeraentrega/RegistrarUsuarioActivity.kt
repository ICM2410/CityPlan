package com.example.primeraentrega

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.primeraentrega.databinding.ActivityRegistrarUsuarioBinding
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.messaging.messaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class RegistrarUsuarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarUsuarioBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef:DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrarUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.guardarperfil.setOnClickListener {
            val user = binding.user.text.toString()
            val password = binding.password.text.toString()
            val telefono = binding.telefono.text.toString().toInt()
            val correo = binding.correo.text.toString()
            val usuario = UsuarioAmigo(user, correo, telefono, 0.0, 0.0, "", false, 0, "", "","")

            // Crea el usuario en la base de datos de Firebase
            guardarUsuarioEnFirebase(usuario, password)

        }
    }
    private fun guardarUsuarioEnFirebase(usuario: UsuarioAmigo, password: String) {
        if (usuario.username.isEmpty() || password.isEmpty() || usuario.telefono.toString().isEmpty() || usuario.email.isEmpty()) {
            Toast.makeText(baseContext, "Por favor, completa todos los campos", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (!validarCorreo(usuario.email)) {
            Toast.makeText(this, "Por favor ingrese una dirección de correo electrónico válida", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Por favor ingrese una contraseña de al menos 6 dígitos", Toast.LENGTH_SHORT).show()
            return
        }

        myRef = database.getReference("Usuario")

        // Verificar si el nodo "Usuario" existe
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Si el nodo "Usuario" no existe, se crea
                    myRef.setValue("placeholder") // Puedes establecer un valor de marcador como "placeholder"
                }

                // Ahora, guarda el usuario en la base de datos de Firebase Realtime Database
                auth.createUserWithEmailAndPassword(usuario.email, password)
                    .addOnSuccessListener { authResult ->
                        // Usuario registrado exitosamente en Firebase Authentication
                        val userId = authResult.user?.uid
                        if (userId != null) {
                            // Asignar el userID al usuario
                            usuario.uid = userId
                            // Ahora, guarda el usuario en la base de datos de Firebase Realtime Database
                            registrarUsuarioEnFirebase(usuario)
                        } else {
                            // No se pudo obtener el userID
                        }
                    }
                    .addOnFailureListener { e ->
                        // Error al registrar usuario en Firebase Authentication
                        Toast.makeText(baseContext, "Error al registrar usuario en Firebase Authentication", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de Firebase Database
            }
        })
    }

    private fun registrarUsuarioEnFirebase(usuario: UsuarioAmigo) {
        // Obtiene el UID del usuario
        val uid = usuario.uid

        // Obteniendo el ID de la imagen desde la carpeta "drawable"
        val drawableResourceId = resources.getIdentifier("contacto_default", "drawable", packageName)

        // Construyendo el URI utilizando el recurso drawable
        val uri = Uri.parse("android.resource://$packageName/$drawableResourceId")

        // Subir la imagen a Firebase Storage
        val storageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("usuarios/$uid.jpg") // Nombre de la imagen en Firebase Storage

        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Obtener el URI de la imagen en Firebase Storage
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                // Asignar el URI a la propiedad imagen del usuario
                usuario.imagen = "usuarios/"+usuario.uid

                // Inicializa la referencia a la base de datos
                val myRef = database.getReference("Usuario")

                Firebase.messaging.token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        // Aquí puedes hacer lo que necesites con el token, como asignarlo al usuario
                        usuario.token = token
                        println("Token guardado correctamente: $token")
                        // Guardar el usuario en Firebase Realtime Database con el UID como clave
                        uid?.let {
                            myRef.child(it).setValue(usuario)
                                .addOnSuccessListener {
                                    // Registro exitoso en Firebase Realtime Database
                                    Toast.makeText(baseContext, "Usuario registrado con éxito", Toast.LENGTH_LONG).show()
                                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                                }
                                .addOnFailureListener { e ->
                                    // Error al registrar en Firebase Realtime Database
                                    Toast.makeText(baseContext, "Error al registrar en Firebase Realtime Database", Toast.LENGTH_SHORT).show()
                                }
                        } ?: run {
                            // Si no se proporciona el UID del usuario
                            Toast.makeText(baseContext, "UID del usuario no válido", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        println("Error al obtener el token: ${task.exception?.message}")
                    }
                }

            }.addOnFailureListener { exception ->
                // Manejar errores al obtener el URI de la imagen
            }
        }.addOnFailureListener { exception ->
            // Manejar errores al subir la imagen a Firebase Storage
        }
    }

    private fun validarCorreo(correo: String): Boolean {
        val regexCorreo = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return regexCorreo.matches(correo)
    }

}