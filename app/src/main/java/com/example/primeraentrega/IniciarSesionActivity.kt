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


            if(inicioUsuario.isEmpty()||inicioPassword.isEmpty()){
                Toast.makeText(this, "Por favor rellene todos los campos ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!validarCorreo(inicioUsuario)) {
                Toast.makeText(this, "Por favor ingrese una dirección de correo electrónico válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(inicioPassword.length<6){
                Toast.makeText(this, "Por favor ingrese una contraseña de al menos 6 digitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                
        }

        binding.buttonHuella.setOnClickListener {
            startActivity(Intent(baseContext, IniciarSesionHuellaActivity::class.java))
        }

        binding.buttonRegistrarse.setOnClickListener {
            startActivity(Intent(baseContext, RegistrarUsuarioActivity::class.java))
        }

    }
    private fun validarCorreo(correo: String): Boolean {
        val regexCorreo = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return regexCorreo.matches(correo)
    }

}
