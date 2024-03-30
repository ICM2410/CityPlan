package com.example.primeraentrega

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.primeraentrega.databinding.ActivityPlanBinding

class PlanActivity : AppCompatActivity() {

    private lateinit var binding : ActivityPlanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBotones();

    }

    private fun  configurarBotones()
    {
        binding.configuraciones.setOnClickListener{
            startActivity(Intent(baseContext,EditarPlanActivity::class.java))
        }

        binding.botonCamara.setOnClickListener{
            startActivity(Intent(baseContext,GaleriaActivity::class.java))
        }
    }

}