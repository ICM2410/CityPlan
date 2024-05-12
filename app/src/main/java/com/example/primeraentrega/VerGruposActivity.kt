package com.example.primeraentrega

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.primeraentrega.Adapters.GroupAdapter
import com.example.primeraentrega.Clases.Grupo
import com.example.primeraentrega.Clases.ListGroup
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.databinding.ActivityVerGruposBinding
import com.example.primeraentrega.Clases.Usuario
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class VerGruposActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerGruposBinding
    private lateinit var database : FirebaseDatabase

    private var isFabOpen=false
    private var rotation=false
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var childId:String?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerGruposBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        databaseReference= FirebaseDatabase.getInstance().getReference("Grupos")
        val usuario = intent.getSerializableExtra("user") as? UsuarioAmigo

        inicializarBotones(usuario)

        auth = FirebaseAuth.getInstance()
        //crearInfoSophie()
        llenarLista()

        binding.gruposList.setOnItemClickListener { parent, view, position, id ->
            val group = groupList[position] // Get the clicked group from the list
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("groupId", group.uid) // Pass the group ID to ChatActivity
            startActivity(intent)
        }

    }

    /*private fun crearInfoSophie() {
        //obtener todos los usuarios
        val userRef = database.getReference("users")
        val listaUsuarios: MutableMap<String, Usuario> = mutableMapOf()
        val listaPlanes: MutableMap<String, Plan> = mutableMapOf()

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    // Obtiene los datos de cada usuario
                    val userId = userSnapshot.key // El ID del usuario
                    val userData = userSnapshot.getValue(Usuario::class.java) // Los datos del usuario convertidos a objeto Usuario

                    // Aquí puedes realizar cualquier operación con los datos del usuario
                    println("ID de usuario: $userId")
                    println("Datos de usuario: $userData")

                    // Agrega el usuario a la lista si los datos no son nulos
                    if (userId != null && userData != null) {
                        listaUsuarios+=(userData.userid to userData)
                    }

                }
                //de ahi se crea un grupo y se guardan ahi todos los usuarios
                val grupo = Grupo(
                    "nos gusta explorar el mundo",
                    "aventureros",
                    "grupos/img1.png",
                    listaUsuarios,
                    listaPlanes
                )
                childId = databaseReference.child("Grupos").push().key

                if (childId != null) {
                    databaseReference.child(childId!!).setValue(grupo).addOnCompleteListener { task ->
                        if (task.isSuccessful) {


                        } else {
                            Toast.makeText(applicationContext, "Fallo en guardar la información del plan", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Maneja el error en caso de que ocurra
                println("Error al obtener los datos del usuario: ${databaseError.message}")
            }
        })


    }*/

    private fun inicializarBotones(usuario: UsuarioAmigo?) {

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    //startActivity(Intent(baseContext, VerGruposActivity::class.java))

                    true
                }
                R.id.cuenta_bar -> {
                    val executor = ContextCompat.getMainExecutor(this)
                    val biometricPrompt = BiometricPrompt(this, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                // Aquí puedes realizar alguna acción después de la autenticación exitosa
                                // Por ejemplo, mostrar un mensaje o iniciar una nueva actividad
                                var intent = Intent(baseContext, PerfilConfActivity::class.java)
                                startActivity(intent)
                                //startActivity(Intent(baseContext, PerfilConfActivity::class.java))
                                //startActivity(Intent(baseContext, VerGruposActivity::class.java))
                                true
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Autenticación de huella dactilar")
                        .setSubtitle("Toque el sensor de huella dactilar")
                        .setNegativeButtonText("Cancelar")
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                    // Respond to navigation item 2 click
                   false
                }
                R.id.salir_bar -> {
                    // Respond to navigation item 3 click
                    startActivity(Intent(baseContext, PerfilConfActivity::class.java))
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                    true
                }
                else -> false
            }
        }

        /*binding.grupoChocmelos.setOnClickListener {
            val intent = Intent(baseContext, ChatActivity::class.java)
            Log.i("idGrupo","revisar Ver grupos $childId")
            intent.putExtra("idGrupo", childId)
            startActivity(intent)
        }*/

        binding.botonAgregarGrupo.setOnClickListener {
            startActivity(Intent(baseContext, AgregarContactosActivity::class.java))
        }

    }

    val groupList: MutableList<ListGroup> = mutableListOf()
    private fun llenarLista() {

        databaseReference = FirebaseDatabase.getInstance().getReference("Groups")

        auth.currentUser?.uid?.let { currentUserUid ->
            databaseReference.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {

                    // Obtener el usuario de dataSnapshot
                    val grupo = dataSnapshot.getValue(Grupo::class.java)
                    Log.e("Referencia", "Aqui llegue a Grupo")
                    Log.e("GrupoImagen", "Imagen: ${grupo?.fotoGrupo}")
                    // Verificar si el usuario no es el usuario actual antes de agregarlo a la lista
                    if (grupo != null && grupo.integrantes.containsKey(currentUserUid)) {

                        Log.e("Referencia", "Apunto de pedir storageRef")
                        val storageRef = FirebaseStorage.getInstance().reference.child(grupo.fotoGrupo)
                        Log.e("Referencia", "Ya pedi")
                        val localfile = File. createTempFile( "tempImage", "jpg")

                        Log.e("GetFile", "Pedire local file")
                        storageRef.getFile(localfile).addOnSuccessListener {
                            Log.e("Entre", "ENTRE")
                            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                            var groupADD= ListGroup(grupo.titulo, dataSnapshot.key, bitmap)
                            groupList.add(groupADD)

                            //Lista
                            val adapter = GroupAdapter(applicationContext,groupList);
                            binding.gruposList.adapter = adapter

                        }.addOnFailureListener{
                            Log.e("Error", "User could not be found")
                        }

                    }
                }
                override fun onChildChanged(dataSnapshot: DataSnapshot, prevChildKey: String?) {

                }
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {

                }
                override fun onChildMoved(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                }
                override fun onCancelled(databaseError: DatabaseError) {
                }
            })
        }

    }

}