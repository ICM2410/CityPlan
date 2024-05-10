package com.example.primeraentrega.Clases

import android.graphics.Bitmap
import java.util.Date

class PlanLista(
    var dateInicio: Date = Date(),
    var dateFinal: Date = Date(),
    var titulo: String = "",
    var fotoEncuentro: String = "",
    var id: String="",
    var actual: String,
) {
    constructor() : this(Date(), Date(), "", "", "","")
}
