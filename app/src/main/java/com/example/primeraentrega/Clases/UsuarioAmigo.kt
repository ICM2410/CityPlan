package com.example.primeraentrega.Clases

class UsuarioAmigo (
    var email: String = "",
    var username: String = "",
    var telefono: Int = 0,
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,
    var uid: String? = null,
    var activo: Boolean = false,
    var cantPasos:Int,
    var huella:String="",
    var Planes: Array<Plan>
)