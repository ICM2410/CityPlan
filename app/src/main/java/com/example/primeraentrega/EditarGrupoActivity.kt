package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.databinding.ActivityEditarGrupoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

class EditarGrupoActivity : AppCompatActivity() {

    private lateinit var binding : ActivityEditarGrupoBinding
    private lateinit var idGrupo : String
    private var idPlan : String=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityEditarGrupoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        idGrupo=intent.getStringExtra("idGrupo").toString()
        inicializarBotones()
    }

    private val SELECCIONAR_FOTO_REQUEST_CODE = 1

    private var isFabOpen=false
    private var rotation=false
    private fun inicializarBotones() {
        binding.buttonAgregarMiembros.setOnClickListener {
            val intent = Intent(this, EditarContactosGrupoActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }


        binding.buttonSalir.setOnClickListener {
            //se sale del grupo
            startActivity(Intent(baseContext, VerGruposActivity::class.java))
        }

        binding.buttonGuardar.setOnClickListener {
            startActivity(Intent(baseContext, ChatActivity::class.java))
        }

        binding.buttonSeleccionarFoto.setOnClickListener {
            val intent = Intent(this@EditarGrupoActivity, SeleccionarFotoActivity::class.java)
            startActivityForResult(intent, SELECCIONAR_FOTO_REQUEST_CODE)
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
            revisarActivo()
        }
    }

    private fun revisarActivo() {
        var existe=false
        val ref = FirebaseDatabase.getInstance().getReference("Grupos")
        ref.child(idGrupo).child("planes").addListenerForSingleValueEvent(object :
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
                    intent.putExtra("idGrupo", idGrupo)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SELECCIONAR_FOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.getStringExtra("imageUri")
            if (imageUri != null) {
                // Cargar la imagen en tu botón o ImageView y aplicar círculo de recorte
                Glide.with(this)
                    .load(Uri.parse(imageUri))
                    .circleCrop() // Aplicar círculo de recorte
                    .into(binding.buttonSeleccionarFoto)
            }
        }
    }
    
}