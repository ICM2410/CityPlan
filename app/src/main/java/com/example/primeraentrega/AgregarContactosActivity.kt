package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricPrompt
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.primeraentrega.Adapters.UserAdapter
import com.example.primeraentrega.Clases.ListUser
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.databinding.ActivityAgregarContactosBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class AgregarContactosActivity : AppCompatActivity() {

    var permiso=false
    val projection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
    //permission val
    val getContactsPermission= registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            permiso=true
           loadPhoneNumbers()
        })

    private lateinit var binding: ActivityAgregarContactosBinding
    private var isFabOpen=false
    private var rotation=false
    private lateinit var idGrupo : String
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        idGrupo = intent.getStringExtra("idGrupo").toString()

        inicializarBotones()
        permissionRequest()

        auth = FirebaseAuth.getInstance()
        evaluarToggle()

        llenarLista()

        binding.agregarGrupo.setOnClickListener() {
            // Retrieve the list of selected users from the adapter
            val selectedUsers = (binding.listaContactos.adapter as? UserAdapter)?.getSelectedUsers()

            // Check if the selectedUsers list is not null
            selectedUsers?.let { users ->
                // Extract the uids from the list of selected users
                val selectedUserIds = users.map { it.uid }

                // Create an intent to navigate to CrearGrupoActivity
                val intent = Intent(this, CrearGrupoActivity::class.java)

                // Put the list of selected user ids as an extra in the intent
                intent.putStringArrayListExtra("selectedUserIds", ArrayList(selectedUserIds))

                // Start the CrearGrupoActivity with the intent
                startActivity(intent)

                // Optionally, show a message to indicate success
                Toast.makeText(this, "Selected users added to group", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun evaluarToggle() {
        binding.toogleButton.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.contactos -> {
                        Toast.makeText(this, "Contactos seleccionados", Toast.LENGTH_SHORT).show()
                        Log.d("ToggleButton", "Contactos seleccionados")

                        //llenar lista de contactos

                            contactList.clear()
                        val adapter = UserAdapter(applicationContext,contactList);
                        binding.listaContactos.adapter = adapter
                            llenarListaTelefonos()


                    }
                    R.id.otros -> {
                        Toast.makeText(this, "Otros seleccionados", Toast.LENGTH_SHORT).show()
                        Log.d("ToggleButton", "Otros seleccionados")
                        contactList.clear()
                        llenarLista()
                        //llenar lista de todos
                    }
                }
            }
        }
    }

    val phoneList: MutableList<String> = mutableListOf()
    private fun loadPhoneNumbers() {
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val phoneNumber = cursor.getString(numberIndex)
                Log.i("telefonos","ver telefonos $phoneNumber")
                phoneList.add(phoneNumber)
            }
        }

        phoneList.forEach { phoneNumber ->
            Log.d("ContactInfo", phoneNumber)
        }

    }

    fun permissionRequest()
    {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_CONTACTS)== PackageManager.PERMISSION_DENIED)
        {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)) {
                // Inside your activity or fragment
                Toast.makeText(getApplicationContext(), "The app requires access to contacts", Toast.LENGTH_LONG).show();
            }

            getContactsPermission.launch(android.Manifest.permission.READ_CONTACTS)
        }
        else
        {
            permiso=true
            loadPhoneNumbers()
        }

    }

    override fun onRestart()
    {
        super.onRestart()
        permissionRequest()
    }

    private fun inicializarBotones() {
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
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabCrearPlan.setOnClickListener {
            var intent = Intent(baseContext, CrearPlanActivity::class.java)
            intent.putExtra("pantalla", "planes")
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabMisPlanes.setOnClickListener {
            var intent = Intent(baseContext, PlanesActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabPlanActivo.setOnClickListener {
            var intent = Intent(baseContext, PlanActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
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

    val contactList: MutableList<ListUser> = mutableListOf()
    private fun llenarLista() {

        databaseReference = FirebaseDatabase.getInstance().getReference("Usuario")

        auth.currentUser?.uid?.let { currentUserUid ->
            databaseReference.addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {

                        // Obtener el usuario de dataSnapshot
                        val usuario = dataSnapshot.getValue(UsuarioAmigo::class.java)
                        Log.e("Referencia", "Aqui llegue a usuario")
                        Log.e("UsuarioImagen", "Imagen: ${usuario?.imagen}")
                        // Verificar si el usuario no es el usuario actual antes de agregarlo a la lista
                        if (usuario != null && dataSnapshot.key != currentUserUid) {

                            Log.e("Referencia", "Apunto de pedir storageRef")
                            val storageRef = FirebaseStorage.getInstance().reference.child("${usuario.imagen}.jpg")
                            Log.e("Referencia", "Ya pedi")
                            val localfile = File. createTempFile( "tempImage", "jpg")

                            Log.e("GetFile", "Pedire local file")
                            storageRef.getFile(localfile).addOnSuccessListener {
                                Log.e("Entre", "ENTRE")
                                Log.e("REFERENCIA", storageRef.toString())
                                val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                                var usuarioADD= ListUser(usuario.username, usuario.uid, bitmap)
                                contactList.add(usuarioADD)

                                //Lista
                                val adapter = UserAdapter(applicationContext,contactList);
                                binding.listaContactos.adapter = adapter

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


    private fun llenarListaTelefonos() {

        databaseReference = FirebaseDatabase.getInstance().getReference("Usuario")

        auth.currentUser?.uid?.let { currentUserUid ->
            databaseReference.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {

                    // Obtener el usuario de dataSnapshot
                    val usuario = dataSnapshot.getValue(UsuarioAmigo::class.java)
                    Log.e("Referencia", "Aqui llegue a usuario")
                    Log.e("UsuarioImagen", "Imagen: ${usuario?.imagen}")
                    // Verificar si el usuario no es el usuario actual antes de agregarlo a la lista

                    if (usuario != null && dataSnapshot.key != currentUserUid && phoneList.contains(usuario.telefono.toString())) {
                        Log.i("telefonos lista", usuario.telefono.toString())

                        Log.e("Referencia 2", "Apunto de pedir storageRef")
                        val storageRef = FirebaseStorage.getInstance().reference.child("${usuario.imagen}.jpg")
                        Log.e("Referencia", "Ya pedi")
                        val localfile = File. createTempFile( "tempImage", "jpg")

                        Log.e("GetFile", "Pedire local file")
                        storageRef.getFile(localfile).addOnSuccessListener {
                            Log.e("Entre", "ENTRE")
                            Log.e("REFERENCIA", storageRef.toString())
                            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                            var usuarioADD= ListUser(usuario.username+" "+ usuario.telefono.toString(), usuario.uid, bitmap)
                            contactList.add(usuarioADD)

                            //Lista
                            val adapter = UserAdapter(applicationContext,contactList);
                            binding.listaContactos.adapter = adapter

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