package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityVerGruposBinding
import com.example.primeraentrega.Clases.Usuario
import com.google.firebase.auth.FirebaseAuth


class VerGruposActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerGruposBinding

    private var isFabOpen=false
    private var rotation=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerGruposBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val usuario = intent.getSerializableExtra("user") as? Usuario

        inicializarBotones(usuario)

    }

    private fun inicializarBotones(usuario: Usuario?) {

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    //startActivity(Intent(baseContext, VerGruposActivity::class.java))

                    true
                }
                R.id.cuenta_bar -> {
                    // Respond to navigation item 2 click
                    //var intent = Intent(baseContext, PerfilConfActivity::class.java)
                    //intent.putExtra("user", usuario)
                   // startActivity(intent)
                    startActivity(Intent(baseContext, PerfilConfActivity::class.java))
                    //startActivity(Intent(baseContext, VerGruposActivity::class.java))
                    true
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

        binding.grupoChocmelos.setOnClickListener {
            startActivity(Intent(baseContext, ChatActivity::class.java))
        }

        binding.botonAgregarGrupo.setOnClickListener {
            startActivity(Intent(baseContext, CrearGrupoActivity::class.java))
        }

    }

}