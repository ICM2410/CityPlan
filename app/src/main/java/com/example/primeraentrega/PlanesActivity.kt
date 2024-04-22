package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.primeraentrega.databinding.ActivityEditarGrupoBinding
import com.example.primeraentrega.databinding.ActivityPlanesBinding
import java.io.File

class PlanesActivity : AppCompatActivity() {
    private lateinit var binding : ActivityPlanesBinding
    private lateinit var idPlan : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPlanesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idPlan=intent.getStringExtra("idPlan").toString()

        inicializarBotones()
    }
    private fun inicializarBotones() {
        binding.botonPlanActivo.setOnClickListener {
            val intent =Intent(baseContext, PlanActivity::class.java)
            intent.putExtra("idPlan",idPlan)
            startActivity(intent)
        }

        binding.botonPlanInactivo.setOnClickListener {
            startActivity(Intent(baseContext, PlanFinalizadoActivity::class.java))
        }

        binding.botonAgregarPlan.setOnClickListener {
            //borrar la informacion del json
            borrarJSON()
            startActivity(Intent(baseContext, CrearPlanActivity::class.java))
        }

    }

    private fun borrarJSON() {
        val filename = "plan.json"
        val file = File(baseContext.getExternalFilesDir(null), filename)

        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                Log.i("BORRAR_ARCHIVO", "Archivo $filename eliminado correctamente.")
            } else {
                Log.e("BORRAR_ARCHIVO", "No se pudo eliminar el archivo $filename.")
            }
        } else {
            Log.w("BORRAR_ARCHIVO", "El archivo $filename no existe.")
        }
    }
}