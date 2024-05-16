package com.example.primeraentrega.Services

import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.primeraentrega.IniciarSesionActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService: FirebaseMessagingService() {
    private val auth=FirebaseAuth.getInstance()
    private val database= FirebaseDatabase.getInstance()
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        //guardar el token del usuario
        val myRef = database.getReference("Usuario")

        // Guardar el usuario en Firebase Realtime Database con el UID como clave

        auth.currentUser?.uid?.let {
            myRef.child(it).child("token").setValue(token)
                .addOnSuccessListener {
                    // Registro exitoso en Firebase Realtime Database
                    Log.i("token","token guardado correctamente")
                }
                .addOnFailureListener {
                    // Error al registrar en Firebase Realtime Database
                    Log.i("token","error al guardar el token")
                }
        }

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        //respond to received messages

    }
}