package com.example.primeraentrega.Clases

import com.example.primeraentrega.Clases.UsuarioAmigo
import java.util.Date

class Plan (
    var dateInicio: Date = Date(),
    var dateFinal: Date = Date(),
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var AmigoMasActivo: Boolean = false,
    var titulo: String = "",
    var fotoEncuentro: String = "",
    var fotopin: String = "",
    var estado:Boolean = false,
    var integrantes: Map<String, PosAmigo> = emptyMap(),
    var id: String="",
    var idAlarma:Int=0
) {
    constructor() : this(Date(), Date(), 0.0, 0.0, false, "", "", "", false, emptyMap(), "",0)
}
