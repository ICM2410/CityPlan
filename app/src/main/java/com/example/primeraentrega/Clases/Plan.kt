package com.example.primeraentrega.Clases

import org.json.JSONObject
import java.util.Date

class Plan (
    val dateInicio: Date,
    val dateFinal: Date,
    val latitude: Double,
    val longitude: Double,
    val AmigoMasActivo: Boolean,
    val titulo: String,
    val fotoEncuentro: String,
    val fotopin: String
) {

    constructor() : this(Date(), Date(), 0.0, 0.0, false, "", "", "")

    fun toJSON(): JSONObject {
        val obj = JSONObject()
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        obj.put("dateInicio", dateInicio.time)
        obj.put("dateFinal", dateFinal.time)
        obj.put("AmigoMasActivo", AmigoMasActivo)
        obj.put("titulo", titulo)
        obj.put("fotoEncuentro", fotoEncuentro)
        obj.put("fotopin", fotopin)
        return obj
    }
}
