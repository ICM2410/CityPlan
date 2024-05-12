package com.example.primeraentrega


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.primeraentrega.Adapters.GroupAdapter
import com.example.primeraentrega.Clases.Grupo
import com.example.primeraentrega.Clases.ListGroup
import com.example.primeraentrega.databinding.ActivityChatBinding
import com.example.primeraentrega.Clases.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class ChatActivity : AppCompatActivity() {

    private lateinit var binding : ActivityChatBinding
    private var isFabOpen=false
    private var rotation=false
    private lateinit var groupID : String
    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
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
    }

    private fun inicializarBotones() {

        binding.configGrupo.setOnClickListener {
            var intent = Intent(baseContext, EditarGrupoActivity::class.java)
            intent.putExtra("idGrupo", groupID)
            startActivity(intent)
        }

        val usuario: Usuario = Usuario()
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
            var intent = Intent(baseContext, PlanActivity::class.java)
            intent.putExtra("idGrupo", groupID)
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

    private fun initializeGroup(groupId: String) {
        val groupReference = FirebaseDatabase.getInstance().getReference("Groups").child(groupId)

        groupReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
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
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors
                Log.e("ChatActivity", "Error retrieving group data: ${error.message}")
            }
        })
    }

}