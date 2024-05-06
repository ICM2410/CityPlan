package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.provider.ContactsContract
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.primeraentrega.Adapters.ContactsAdapter
import com.example.primeraentrega.databinding.ActivityAgregarContactosBinding
import com.example.primeraentrega.Clases.Usuario
import com.google.firebase.auth.FirebaseAuth

class AgregarContactosActivity : AppCompatActivity() {

    //Permission val
    val getContactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            updateUI(it)
        })

    private lateinit var binding: ActivityAgregarContactosBinding
    private lateinit var adapter : ContactsAdapter
    val projection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
    private var isFabOpen=false
    private var rotation=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = ContactsAdapter(this, null, 0)
        binding.listaContactos.adapter=adapter
        permissionRequest()
        inicializarBotones()
    }

    private fun inicializarBotones() {
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
            startActivity(Intent(baseContext, PlanesPasadosActivity::class.java))
        }

        binding.fabCrearPlan.setOnClickListener {
            startActivity(Intent(baseContext, CrearPlanActivity::class.java))
        }

        binding.fabMisPlanes.setOnClickListener {
            startActivity(Intent(baseContext, PlanesActivity::class.java))
        }

        binding.fabPlanActivo.setOnClickListener {
            startActivity(Intent(baseContext, PlanActivity::class.java))
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

    fun permissionRequest(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED){
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)){
                Toast.makeText(this, "The app requires access to the contacts", Toast.LENGTH_LONG).show()
            }
            getContactsPermission.launch(android.Manifest.permission.READ_CONTACTS)
        }else{
            updateUI(true)
        }
    }

    fun updateUI(contacts : Boolean){
        if(contacts){
            //Permission Granted
            var cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null)
            adapter.changeCursor(cursor)

        }else {
            //Permission Denied

        }
    }
}