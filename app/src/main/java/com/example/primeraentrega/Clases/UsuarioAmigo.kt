package com.example.primeraentrega.Clases

import java.io.Serializable

class UsuarioAmigo : Serializable{
    var email: String = ""
    var username: String = ""
    var telefono: Int = 0
    var latitud: Double = 0.0
    var longitud: Double = 0.0
    var uid: String? = null
    var activo: Boolean = false
    var cantPasos: Int=0
    var huella: String = ""
    var imagen: String = ""
    var token: String=""

    constructor()

    constructor(username:String, email:String, telefono:Int, latitud:Double, longitud:Double, uid:String,
        activo:Boolean, cantPasos:Int, huella:String, imagen:String,token: String){

        this.username=username
        this.email=email
        this.telefono=telefono
        this.latitud=latitud
        this.longitud=longitud
        this.uid=uid
        this.activo=activo
        this.cantPasos=cantPasos
        this.huella=huella
        this.imagen=imagen
        this.token=token
    }
}