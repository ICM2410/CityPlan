package com.example.primeraentrega

import android.content.ContentValues.TAG
import android.content.Intent
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding
import com.example.primeraentrega.databinding.ActivityRegistrarUsuarioBinding
import com.example.primeraentrega.usuario.usuario
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

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
            val telefono = binding.telefono.text.toString()
            val correo = binding.correo.text.toString()

            val usuario = usuario(user,telefono, password,"", correo, "")


            // Crea el usuario en la base de datos de Firebase
            guardarUsuarioEnFirebase(usuario)

        }
    }
    private fun guardarUsuarioEnFirebase(usuario: usuario) {

        auth.createUserWithEmailAndPassword(usuario.correo, usuario.password)
            .addOnSuccessListener { authResult ->
                // Usuario registrado exitosamente en Firebase Authentication
                val userId = authResult.user?.uid
                if (userId != null) {
                    // Asignar el userID al usuario
                    usuario.userid = userId
                    // Ahora, guarda el usuario en la base de datos de Firebase Realtime Database
                    registrarUsuarioEnFirebase(usuario)
                } else {
                    // No se pudo obtener el userID
                    Toast.makeText(baseContext, "Error: No se pudo obtener el userID", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // Error al registrar usuario en Firebase Authentication
                Toast.makeText(baseContext, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun registrarUsuarioEnFirebase(usuario: usuario) {
        // Validación de campos
        if (usuario.user.isEmpty() || usuario.password.isEmpty() || usuario.telefono.isEmpty() || usuario.correo.isEmpty()) {
            Toast.makeText(baseContext, "Por favor, completa todos los campos", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Inicializa la referencia a la base de datos
        myRef = database.getReference("users") // Aquí, especifica la ubicación correcta en la base de datos

        // Guardar el usuario en Firebase Realtime Database
        val key = myRef.push().key
        key?.let {
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
        }
    }

}