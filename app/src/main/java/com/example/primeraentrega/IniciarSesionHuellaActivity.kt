package com.example.primeraentrega

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.primeraentrega.databinding.ActivityIniciarSesionHuellaBinding
import com.example.primeraentrega.Clases.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class IniciarSesionHuellaActivity : AppCompatActivity() {


    private lateinit var binding: ActivityIniciarSesionHuellaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIniciarSesionHuellaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val usuario = intent.getSerializableExtra("user") as? Usuario

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Solicitar la huella dactilar del usuario
        solicitarHuella(usuario)
    }

    private fun solicitarHuella(usuario: Usuario?) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Aquí puedes generar un ID único basado en los datos biométricos
                    val biometricData = result.cryptoObject?.cipher?.iv ?: ByteArray(0)
                    val biometricId = generateBiometricId(biometricData)

                    // Asignar el ID de la huella dactilar al usuario
                    usuario?.fingerprintId = biometricId


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

    private fun guardarUsuarioEnFirebase(usuario: Usuario?) {

    }

}