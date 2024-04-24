package com.example.primeraentrega

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding

import com.example.primeraentrega.usuario.usuario
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue


class IniciarSesionActivity : AppCompatActivity() {

    private lateinit var binding : ActivityIniciarSesionBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef:DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityIniciarSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        inicializarBotones()
    }

    private fun inicializarBotones() {
        binding.buttonIniciarSesion.setOnClickListener {
            val inicioUsuario = binding.user.text.toString()
            val inicioPassword = binding.password.text.toString()

            auth.signInWithEmailAndPassword(inicioUsuario, inicioPassword)
                .addOnSuccessListener { authResult ->
                    // Inicio de sesión exitoso
                    val userId = authResult.user?.uid
                    var intent= Intent(baseContext, VerGruposActivity::class.java)

                    startActivity(intent)
                    // Aquí puedes agregar lógica adicional después del inicio de sesión exitoso
                }
                .addOnFailureListener { e ->
                    // Error en el inicio de sesión
                    Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            /* myRef = database.getReference("users")
            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var usuarioEncontrado = false
                    for (child in snapshot.children) {
                        val user = child.getValue<usuario>()
                        if (user?.user.toString() == inicioUsuario && user?.password.toString() == inicioPassword) {
                            // Si se encuentra el usuario, iniciar la actividad y cambiar la bandera a true
                            var intent= Intent(baseContext, VerGruposActivity::class.java)
                            intent.putExtra("user", user)
                            startActivity(intent)
                            usuarioEncontrado = true
                            break  // Salir del bucle si se encuentra el usuario
                        }
                    }

                    // Si el usuario no se encuentra después de iterar sobre todos los usuarios, mostrar un mensaje de error
                    if (!usuarioEncontrado) {
                        Toast.makeText(baseContext, "Credenciales inválidas", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }
*/
                
        }

        binding.buttonHuella.setOnClickListener {
            startActivity(Intent(baseContext, IniciarSesionHuellaActivity::class.java))
        }

        binding.buttonRegistrarse.setOnClickListener {
            startActivity(Intent(baseContext, RegistrarUsuarioActivity::class.java))
        }

    }

}
