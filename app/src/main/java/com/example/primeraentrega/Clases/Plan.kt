package com.example.primeraentrega.Clases

import kotlinx.coroutines.CoroutineStart
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import android.util.Base64

class Plan (val dateInicio : Date,val dateFinal : Date, val latitude: Double,
            val longitude: Double, val AmigoMasActivo: Boolean, val titulo: String, val fotoEncuentro: ByteArray?, val fotopin: ByteArray?){

    fun toJSON() : JSONObject {
        val obj = JSONObject();
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        obj.put("dateInicio",dateInicio.time )
        obj.put("dateFinal",dateFinal.time )
        obj.put("AmigoMasActivo",AmigoMasActivo )
        obj.put("titulo",titulo )
        obj.put("fotoEncuentro", fotoEncuentro?.let { Base64.encodeToString(it,
            Base64.DEFAULT
        ) })
        obj.put("fotopin", fotopin?.let { Base64.encodeToString(it, Base64.DEFAULT) })
        return obj
    }

}