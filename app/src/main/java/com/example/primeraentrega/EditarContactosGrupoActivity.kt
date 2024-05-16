package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class EditarContactosGrupoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgregarContactosBinding
    val projection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
    private var isFabOpen=false
    private var rotation=false
    private lateinit var idGrupo : String
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener el ID del grupo de los extras del intent
        idGrupo = intent.getStringExtra("idGrupo").toString()

        //permissionRequest()
        inicializarBotones()

        // Define and initialize the onUserSelectedListener
        val onUserSelectedListener = this

        auth = FirebaseAuth.getInstance()

        llenarLista()

        binding.agregarGrupo.setOnClickListener() {
            agregarUsuariosAGrupo()
        }
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
                    // Respond to navigation item 2 click
                    var intent = Intent(baseContext, PerfilConfActivity::class.java)
                    intent.putExtra("user", usuario)
                    startActivity(intent)
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

    /*fun permissionRequest(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED){
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)){
                Toast.makeText(this, "The app requires access to the contacts", Toast.LENGTH_LONG).show()
            }
            getContactsPermission.launch(android.Manifest.permission.READ_CONTACTS)
        }else{
            updateUI(true)
        }
    }*/

    /*fun updateUI(contacts : Boolean){
        if(contacts){
            //Permission Granted
            var cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null)
            adapter.changeCursor(cursor)

        }else {
            //Permission Denied

        }
    }*/

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

    private fun agregarUsuariosAGrupo() {
        // Obtener la lista de usuarios seleccionados del adaptador
        val selectedUsers = (binding.listaContactos.adapter as? UserAdapter)?.getSelectedUsers()

        // Verificar si hay usuarios seleccionados
        if (!selectedUsers.isNullOrEmpty()) {
            // Referencia al grupo en la base de datos
            val grupoRef = FirebaseDatabase.getInstance().getReference("Groups").child(idGrupo)

            // Iterar sobre los usuarios seleccionados y agregarlos al grupo
            selectedUsers.forEach { user ->
                user.uid?.let { userId ->
                    val miembrosRef = grupoRef.child("integrantes").child(userId)
                    miembrosRef.setValue(userId)
                        .addOnSuccessListener {
                            Toast.makeText(this, "${user.nombre} agregado al grupo", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al agregar ${user.nombre} al grupo: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            // Limpiar la selección después de agregar usuarios al grupo
            (binding.listaContactos.adapter as? UserAdapter)?.clearSelection()
        } else {
            Toast.makeText(this, "No se han seleccionado usuarios para agregar al grupo", Toast.LENGTH_SHORT).show()
        }
    }

}