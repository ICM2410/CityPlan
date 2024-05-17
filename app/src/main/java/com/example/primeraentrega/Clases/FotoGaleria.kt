package com.example.primeraentrega.Clases

import java.util.Date

class FotoGaleria (
    var fecha: Date = Date(),
    var direccionImagen: String="",
){
    constructor() : this(Date(),"")
}