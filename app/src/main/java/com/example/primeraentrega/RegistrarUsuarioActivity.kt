package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.primeraentrega.databinding.ActivityRegistrarUsuarioBinding
import com.example.primeraentrega.Clases.Usuario
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue

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

            val usuario = Usuario(user,telefono, password,"", correo, "")


            // Crea el usuario en la base de datos de Firebase
            guardarUsuarioEnFirebase(usuario)

        }
    }
    private fun guardarUsuarioEnFirebase(usuario: Usuario) {
        if (usuario.user.isEmpty() || usuario.password.isEmpty() || usuario.telefono.isEmpty() || usuario.correo.isEmpty()) {
            Toast.makeText(baseContext, "Por favor, completa todos los campos", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (!validarCorreo(usuario.correo)) {
            Toast.makeText(this, "Por favor ingrese una dirección de correo electrónico válida", Toast.LENGTH_SHORT).show()
            return
        }

        if(usuario.password.length<6){
            Toast.makeText(this, "Por favor ingrese una contraseña de al menos 6 digitos", Toast.LENGTH_SHORT).show()
            return
        }
        myRef = database.getReference("users")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var usuarioEncontrado = false
                for (child in snapshot.children) {
                    val user = child.getValue<Usuario>()

                    if(user?.user.toString()!=usuario.user.toString() && user?.telefono.toString()!=usuario.telefono){
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
                                }
                            }
                            .addOnFailureListener { e ->
                                // Error al registrar usuario en Firebase Authentication

                            }
                    }
                    else{
                        Toast.makeText(baseContext, "Error al registrar ", Toast.LENGTH_SHORT).show()

                    }
                }
            }
                override fun onCancelled(error: DatabaseError) {


                }
            })





    }

    private fun registrarUsuarioEnFirebase(usuario: Usuario) {
        // Validación de campos


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
    private fun validarCorreo(correo: String): Boolean {
        val regexCorreo = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return regexCorreo.matches(correo)
    }

}