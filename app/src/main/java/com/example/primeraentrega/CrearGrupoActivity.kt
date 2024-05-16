package com.example.primeraentrega

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.primeraentrega.Clases.Grupo
import com.bumptech.glide.Glide
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.Notifications.FcmApi
import com.example.primeraentrega.Notifications.GroupState
import com.example.primeraentrega.Notifications.NotificationBody
import com.example.primeraentrega.Notifications.PlanState
import com.example.primeraentrega.Notifications.SendMessageDTO
import com.example.primeraentrega.databinding.ActivityCrearGrupoBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.messaging
import com.google.firebase.storage.FirebaseStorage

import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class CrearGrupoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearGrupoBinding
    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth
    private lateinit var selectedUserIds: MutableMap<String?, String?>
    lateinit var uriCamera : Uri

    val getContentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback {
            loadImage(it!!)
        })

    val getContentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),ActivityResultCallback {
            if(it){
                loadImage(uriCamera)
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearGrupoBinding.inflate(layoutInflater)
        setContentView(binding.root)


        auth = FirebaseAuth.getInstance()


        // Initialize selectedUserIds with an empty mutable map
        selectedUserIds = mutableMapOf()
        // Check if the intent contains the extra
        if (intent.hasExtra("selectedUserIds")) {
            // Retrieve the list of selected user ids from the intent extras
            val selectedUserIdsStrings = intent.getStringArrayListExtra("selectedUserIds")
            // Check if the list is not null
            if (selectedUserIdsStrings != null) {
                selectedUserIdsStrings.add(FirebaseAuth.getInstance().currentUser?.uid)
                // Iterate through the list and do whatever you need with the user ids
                for (userId in selectedUserIdsStrings) {
                    // Ensure the key is not null
                    selectedUserIds[userId] = userId
                    // Print the user id for demonstration
                    Log.e("CrearGrupoActivity", "Selected user id: $userId")
                }
            } else {
                // Handle the case when the list is null
                Log.e("CrearGrupoActivity", "No selected user ids found")
            }
        }

        inicializarBotones()

    }

    private val SELECCIONAR_FOTO_REQUEST_CODE = 1

    private fun inicializarBotones() {

        val file = File(getFilesDir(), "picFromCamera");
        uriCamera =  FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)

        binding.botonGaleria.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        binding.botonCamara.setOnClickListener {
            getContentCamera.launch(uriCamera)
        }


        binding.buttonGuardar.setOnClickListener {
            createGroup()
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
                    val executor = ContextCompat.getMainExecutor(this)
                    val biometricPrompt = BiometricPrompt(this, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                // Aquí puedes realizar alguna acción después de la autenticación exitosa
                                // Por ejemplo, mostrar un mensaje o iniciar una nueva actividad
                                var intent = Intent(baseContext, PerfilConfActivity::class.java)
                                intent.putExtra("user", usuario)
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
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                    true
                }
                else -> false
            }
        }

    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECCIONAR_FOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.getStringExtra("imageUri")
            if (imageUri != null) {
                // Cargar la imagen en tu botón o ImageView y aplicar círculo de recorte
                Glide.with(this)
                    .load(Uri.parse(imageUri))
                    .circleCrop() // Aplicar círculo de recorte
                    .into(binding.ButtonSeleccionarFoto)
            }
        }
    }*/

    private fun uploadGroupImage(groupId: String) {
        var imageUri: Uri? = null

        val drawableFoto = binding.fotoSeleccionada.drawable
        //var imgUrlplan: String? =null
        Log.e("GROUPID",groupId)
        storageReference = FirebaseStorage.getInstance().getReference("Groups/$groupId")

        if (drawableFoto != null) {
            if (drawableFoto is BitmapDrawable) {
                // Si el Drawable es un BitmapDrawable, puedes obtener el Bitmap y luego su URI
                val bitmap = drawableFoto.bitmap
                imageUri = bitmapToUri(this, bitmap)
                storageReference.putFile(imageUri).addOnSuccessListener {
                    FirebaseDatabase.getInstance().getReference("Groups").child(groupId)
                        .child("fotoGrupo").setValue("Groups/$groupId")
                        .addOnSuccessListener {
                            Log.e("FOTO", "Foto uploaded correctly")
                            // Start the VerGruposActivity
                            startActivity(Intent(this, VerGruposActivity::class.java))
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Failed to update group photo: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                }.addOnFailureListener()
                {
                    Toast.makeText(this, "Fallo en guardar la informacion del usuario", Toast.LENGTH_LONG ).show()
                }

            }
        }
    }

    private fun bitmapToUri(context: Context, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    private fun loadImage(uri : Uri?) {
        val imageStream = getContentResolver().openInputStream(uri!!)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.fotoSeleccionada.setImageBitmap(bitmap)


    }

    private fun createGroup() {
        var groupName = binding.editTextNombreGrupo.text.toString()
        var groupDescription = binding.editTextDescGrupo.text.toString()

        if(validateForm(groupName,groupDescription)) {
            //Create a NuevoGrupo object
            val group = Grupo(
                descripcion = groupDescription,
                titulo = groupName,
                fotoGrupo = "",
                integrantes = selectedUserIds,
                planes = emptyMap(),
                mensajes = emptyMap()
            )
            val groupsRef = FirebaseDatabase.getInstance().getReference("Groups")


            // Push the new group object to the "Groups" node
            val newGroupRef = groupsRef.push()
            Log.e("GROUP REFERENCE", groupsRef.toString())

            newGroupRef.setValue(group).addOnSuccessListener {
                    // Group creation successful
                    Log.d("setValue", "Data written successfully")

                    // Retrieve the ID assigned by Firebase
                    val groupId = newGroupRef.key
                    Log.e("ID", "GroupID - $groupId")
                    if (groupId != null) {
                        // Upload the group image with the retrieved group ID
                        uploadGroupImage(groupId)
                        //enviar notificacion de grupo creado

                        notificacionGrupoCreado(groupId)
                    } else {
                        Log.e("NO ID", "Group ID is null")
                    }
            }.addOnFailureListener { exception ->
                // Group creation failed
                Log.e("NO GROUP", "Error creating group", exception)
            }

        }
    }

    private val api: FcmApi = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create()
    private fun notificacionGrupoCreado(groupId: String) {
        //subscribirme a notificaciones del nuevo grupo
        subscribirACanal(groupId)
        //enviar notificaciones a todos sus nuevos miembros
        val userRef=FirebaseDatabase.getInstance().getReference("Usuario")
        for ((key, _) in selectedUserIds) {

            if (key != null) {
                userRef.child(key).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userData = snapshot.getValue(UsuarioAmigo::class.java)
                        if(userData!=null)
                        {
                            //enviar notificacion
                            val isBroadcast=false
                            var state = GroupState(
                                true,
                                userData.token,
                                "El grupo ${binding.editTextNombreGrupo.text.toString()} se ha creado",
                                groupId)
                            val message= SendMessageDTO(
                                to=if(isBroadcast) "1" else state.remoteToken,
                                notification = NotificationBody(
                                    title = "Nuevo Grupo!",
                                    body = state.messageText,
                                    id = "0",
                                    alarmId = 0,
                                    idGrupo = state.idGrupo
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
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
            }
        }
    }

    private fun subscribirACanal(canal:String) {
        //aqui se debe subscribir a todos los chats a los que pertenece
        Firebase.messaging.subscribeToTopic(canal).addOnSuccessListener {
            Log.i("subscripcion", "Existosa")
        }.addOnFailureListener{
            Log.e("subscripcion", "ERROR")
        }
    }

    private fun validateForm(groupName: String, groupDescription: String): Boolean {
        var valid = false
        if (groupName.isEmpty()){
            binding.editTextNombreGrupo.setError ("Required!")
        }else if (groupDescription.isEmpty()){
            binding.editTextDescGrupo.setError ("Required!")
        }else{
            valid = true
        }
        return valid
    }
}
