package com.example.primeraentrega.Clases



import java.util.Date

class Mensaje(
    var mensaje: String = "",
    var emisor: String = "",
    var createdAt: Long = 0
) {
    constructor(mensaje: String, emisor: String) : this(mensaje, emisor, System.currentTimeMillis())
}