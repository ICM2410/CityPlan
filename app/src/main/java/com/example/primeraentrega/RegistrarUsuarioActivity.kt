package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

            val usuario = usuario(user,telefono, password)
            // Crea el usuario en la base de datos de Firebase
            registrarUsuarioEnFirebase(usuario)

        }
    }

    private fun registrarUsuarioEnFirebase(usuario: usuario) {
        // Validación de campos
        if (usuario.user.isEmpty() || usuario.password.isEmpty() || usuario.telefono.isEmpty()) {
            Toast.makeText(baseContext, "Por favor, completa todos los campos", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Inicializa la referencia a la base de datos
        myRef = database.getReference("users") // Aquí, especifica la ubicación correcta en la base de datos

        // Validación de contraseña


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