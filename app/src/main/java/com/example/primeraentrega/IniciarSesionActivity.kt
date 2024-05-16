package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.databinding.ActivityIniciarSesionBinding
import com.google.firebase.Firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.messaging

import java.security.MessageDigest


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
                    val user=authResult.user
                    actualizarMiToken(user)

                }
                .addOnFailureListener { e ->
                    // Error en el inicio de sesión
                    Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                
        }


        binding.buttonRegistrarse.setOnClickListener {
            startActivity(Intent(baseContext, RegistrarUsuarioActivity::class.java))
        }

    }

    private fun actualizarMiToken(user: FirebaseUser?) {
        //aqui se debe subscribir a todos los chats a los que pertenece
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result

                // Aquí puedes hacer lo que necesites con el token, como asignarlo al usuario
                println("Token guardado correctamente: $token")
                val myRef = database.getReference("Usuario")
                // Guardar el usuario en Firebase Realtime Database con el UID como clave
                auth.currentUser?.uid?.let {
                    myRef.child(it).child("token").setValue(token)
                        .addOnSuccessListener {
                            // Registro exitoso en Firebase Realtime Database
                            var intent= Intent(baseContext, VerGruposActivity::class.java)
                            intent.putExtra("user", user)

                            startActivity(intent)
                        }
                }

            } else {
                println("Error al obtener el token: ${task.exception?.message}")
            }
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

    private fun validarCorreo(correo: String): Boolean {
        val regexCorreo = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return regexCorreo.matches(correo)
    }

}
