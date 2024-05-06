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
}
