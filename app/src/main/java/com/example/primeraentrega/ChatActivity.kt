package com.example.primeraentrega


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.primeraentrega.Adapters.ChatAdapter
import com.example.primeraentrega.Clases.Grupo
import com.example.primeraentrega.Clases.ListMessage
import com.example.primeraentrega.Clases.Mensaje
import com.example.primeraentrega.databinding.ActivityChatBinding
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.Notifications.ChatState
import com.example.primeraentrega.Notifications.FcmApi
import com.example.primeraentrega.Notifications.GroupState
import com.example.primeraentrega.Notifications.NotificationBody
import com.example.primeraentrega.Notifications.SendMessageDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.io.IOException

import java.util.Date


class ChatActivity : AppCompatActivity() {

    private lateinit var binding : ActivityChatBinding
    private var isFabOpen=false
    private var rotation=false
    private lateinit var groupID : String
    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var idPlan : String=""
  
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        groupID = intent.getStringExtra("groupId").toString()
        inicializarBotones()
        //binding.bottomNavigation.selectedItemId = R.id.cuenta_bar // Establecer elemento seleccionado
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        initializeGroup(groupID)
        binding.botonEnviar.setOnClickListener{
            sendMessage()
            Log.e("ENVIADO", "El mensaje fue enviado")
        }
    }

    private fun inicializarBotones() {

        binding.configGrupo.setOnClickListener {
            var intent = Intent(baseContext, EditarGrupoActivity::class.java)
            intent.putExtra("idGrupo", groupID)
            startActivity(intent)
        }

        val usuario: UsuarioAmigo = UsuarioAmigo()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    startActivity(Intent(baseContext, VerGruposActivity::class.java))

                    true
                }
                R.id.cuenta_bar -> {
                    // Respond to navigation item 2 click
                    var intent = Intent(baseContext, PerfilConfActivity::class.java)
                    intent.putExtra("user", usuario)
                    startActivity(intent)
                    //startActivity(Intent(baseContext, VerGruposActivity::class.java))
                    true
                }
                R.id.salir_bar -> {
                    // Respond to navigation item 3 click
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                    true
                }
                else -> false
            }
        }

        initShowout(binding.pasadosView)
        initShowout(binding.nuevoView)
        initShowout(binding.activoView)
        initShowout(binding.planesView)
        binding.fabMenuPlan.setOnClickListener {
            if(!isFabOpen)
            {
                showFabMenu();
            }
            else
            {
                closeFabMenu();
            }
        }

        fabClicks()
    }

    private fun fabClicks() {
        binding.fabPlanesPasados.setOnClickListener {
            var intent = Intent(baseContext, PlanesPasadosActivity::class.java)
            intent.putExtra("idGrupo", groupID)
            startActivity(intent)
        }

        binding.fabCrearPlan.setOnClickListener {
            var intent = Intent(baseContext, CrearPlanActivity::class.java)
            intent.putExtra("pantalla", "planes")
            intent.putExtra("idGrupo", groupID)
            startActivity(intent)
        }

        binding.fabMisPlanes.setOnClickListener {
            var intent = Intent(baseContext, PlanesActivity::class.java)
            intent.putExtra("idGrupo", groupID)
            startActivity(intent)
        }

        binding.fabPlanActivo.setOnClickListener {
            revisarActivo()
        }
    }

    private fun revisarActivo() {
        var existe=false
        val ref = FirebaseDatabase.getInstance().getReference("Grupos")
        ref.child(groupID).child("planes").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    // Obtiene los datos de cada usuario
                    val planId = userSnapshot.key // El ID del usuario
                    val planData = userSnapshot.getValue(Plan::class.java) // Los datos del usuario convertidos a objeto Usuario

                    // Aquí puedes realizar cualquier operación con los datos del usuario
                    println("ID de usuario: $planId")
                    println("Datos de usuario: $planData")

                    // Crea un objeto PosAmigo con la información del usuario
                    var status=""
                    val plan = planData?.let {
                        status=planAcrivo(planData.dateInicio,planData.dateFinal)
                    }

                    // Si el usuario y su ID no son nulos, añádelos al mapa integrantesMap
                    if (planId != null &&  plan != null && status!="Activo") {
                        existe=true
                        idPlan=planId
                    }
                }

                if(existe)
                {
                    var intent = Intent(baseContext, PlanActivity::class.java)
                    intent.putExtra("idGrupo", groupID)
                    intent.putExtra("idPlan", idPlan)
                    startActivity(intent)
                }
                else
                {
                    Toast.makeText(applicationContext, "No hay planes activos", Toast.LENGTH_LONG).show()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Maneja el error en caso de que ocurra
                println("Error al obtener los datos de planes: ${databaseError.message}")
            }
        })
    }

    private fun planAcrivo(dateInicio: java.util.Date, dateFinal: java.util.Date): String {
        val fechaActual = Date()

        return when {
            fechaActual.before(dateInicio) -> "Activo"
            fechaActual.after(dateFinal) -> "Cerrado"
            else -> "Abierto"
        }
    }

    private fun initShowout (v: View){
        v.apply {
            visibility = View. GONE
            translationY= height.toFloat()
            alpha = 0f
        }
    }

    private fun closeFabMenu() {
        rotation=rotateFAB()
        isFabOpen=false
        cerrar(binding.pasadosView)
        cerrar(binding.nuevoView)
        cerrar(binding.activoView)
        cerrar(binding.planesView)
    }

    private fun cerrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=1f
            translationY=0f
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        visibility= View.GONE
                    }
                })
                .alpha(0f)
                .start()
        }
    }

    private fun showFabMenu() {
        rotation=rotateFAB()
        isFabOpen=true

        //motrar info
        mostrar(binding.pasadosView)
        mostrar(binding.nuevoView)
        mostrar(binding.activoView)
        mostrar(binding.planesView)

    }

    private fun mostrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=0f
            translationY=height.toFloat()
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){})
                .alpha(1f)
                .start()
        }
    }

    private fun rotateFAB():Boolean {
        binding.fabMenuPlan.animate()
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter(){})
            .rotation(if(!isFabOpen) 180f else 0f)

        return isFabOpen
    }
    private fun sendMessage() {
        var mensajeTexto = binding.espacioDeTexto.text.toString()
        if (mensajeTexto.isNotEmpty()) {
            // Get the UID of the current user
            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserUid != null) {
                val mensaje = Mensaje(mensajeTexto, currentUserUid)
                // Get a reference to the "Messages" node under the group's node
                val groupMessagesRef = FirebaseDatabase.getInstance().getReference("Groups").child(groupID).child("mensajes")
                // Push the new message to the "Messages" node
                groupMessagesRef.push().setValue(mensaje)
                    .addOnSuccessListener {
                        // Message sent successfully
                        // Optionally, clear the message input field or show a success message
                        binding.espacioDeTexto.text.clear()

                        //se envia notificacion del mensaje a todos los del grupo
                        enviarNotificacion(groupID,mensajeTexto)
                    }
                    .addOnFailureListener { exception ->
                        // Handle any errors while sending the message
                        Log.e("ChatActivity", "Error sending message: ${exception.message}")
                    }
            }
        }
    }

    private val api: FcmApi = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create()
    private fun enviarNotificacion(groupID: String, mensajeTexto: String) {
        auth.currentUser?.let {
            val currentUser = FirebaseDatabase.getInstance().getReference("Usuario").child(it.uid)

            currentUser.addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.getValue(UsuarioAmigo::class.java)
                    //se envia la notificacion
                    val isBroadcast=true
                    var state = ChatState(
                        true,
                        "",
                        " ${userData?.username}:  $mensajeTexto",
                        groupID)
                    val message= SendMessageDTO(
                        to=if(isBroadcast) "1" else state.remoteToken,
                        notification = NotificationBody(
                            title = "${binding.nombreGrupoChat.text} Nuevo Mensaje!",
                            body = state.messageText,
                            id = "0",
                            alarmId = 0,
                            idGrupo = state.idChat
                        )
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            if (isBroadcast){
                                api.broadcast(message)
                            } else {
                                api.sendMessage(message)
                            }
                        } catch (e: HttpException) {
                            e.printStackTrace()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }
    private fun initializeGroup(groupId: String) {
        val groupReference = FirebaseDatabase.getInstance().getReference("Groups").child(groupId)

        groupReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.e("DATASNAPSHOT", "Revisare lo que hay en la base de datos")
                    val grupo = dataSnapshot.getValue(Grupo::class.java)
                    if (grupo != null) {
                        // Update UI with the new data
                        binding.nombreGrupoChat.text = grupo.titulo

                        // Load the image from Firebase Storage
                        val storageRef = FirebaseStorage.getInstance().reference.child(grupo.fotoGrupo)
                        val localFile = File.createTempFile("tempImage", "jpg")
                        storageRef.getFile(localFile).addOnSuccessListener {
                            // Load the downloaded image into the ImageView
                            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                            binding.imagenGrupoChat.setImageBitmap(bitmap)
                        }.addOnFailureListener { exception ->
                            // Handle any errors while downloading the image
                            Log.e("ChatActivity", "Error downloading group image: ${exception.message}")
                        }
                        // Listen for new messages
                        initializeMessageListener(groupId)

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors
                Log.e("ChatActivity", "Error retrieving group data: ${error.message}")
            }
        })
    }

    private lateinit var mensajesGrupo: MutableList<ListMessage> // Updated to hold ListMessage objects

    private fun initializeMessageListener(groupId: String) {
        mensajesGrupo = mutableListOf()

        val groupMessagesRef = FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("mensajes")

        groupMessagesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val mensaje = snapshot.getValue(Mensaje::class.java)
                if (mensaje != null) {
                    val emisorUid = mensaje.emisor
                    var nombreEmisor =""
                    var bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                    getUser(emisorUid) { usuario ->
                        if (usuario != null) {
                            nombreEmisor = usuario.username
                            Log.e("EMISOR", "Nombre emisor $nombreEmisor")
                            val storageRef = FirebaseStorage.getInstance().reference.child("${usuario.imagen}.jpg")
                            val localfile = File. createTempFile( "tempImage", "jpg")
                            storageRef.getFile(localfile).addOnSuccessListener {
                                bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                                val listMessage = ListMessage(
                                    snapshot.key,
                                    bitmap,
                                    mensaje.mensaje,
                                    emisorUid,
                                    nombreEmisor,
                                    mensaje.createdAt
                                )

                                // Add the ListMessage object to the list
                                mensajesGrupo.add(listMessage)
                                // Update the adapter
                                val adapter = ChatAdapter(applicationContext, mensajesGrupo, FirebaseAuth.getInstance().currentUser!!.uid)
                                binding.chat.adapter = adapter

                            }.addOnFailureListener{
                                Log.e("Error", "User could not be found")
                            }
                        } else {
                            // Handle the case where the user object could not be retrieved
                            println("Failed to retrieve user")
                        }
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle the case where a message is changed (if needed)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle the case where a message is removed (if needed)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle the case where a message is moved (if needed)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors
                Log.e("ChatActivity", "Error retrieving group messages: ${error.message}")
            }
        })
    }

    fun getUser(uid: String, callback: (UsuarioAmigo?) -> Unit) {
        val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("Usuario").child(uid)

        databaseReference.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSnapshot: DataSnapshot? = task.result
                val usuario: UsuarioAmigo? = dataSnapshot?.getValue(UsuarioAmigo::class.java)
                callback(usuario)
            } else {
                // Handle errors
                callback(null)
            }
        }
    }




}