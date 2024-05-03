package com.example.primeraentrega.usuario

import java.io.Serializable

class Usuario : Serializable{
    lateinit var user:String
    lateinit var telefono:String
    lateinit var password:String
    lateinit var correo:String
    lateinit var userid:String
    var fingerprintId: String? = null

    constructor()

    constructor(user:String, telefono:String,password:String, correo:String, userid:String){
        this.user=user
        this.telefono=telefono
        this.password=password
        this.correo=correo
        this.userid=userid
    }

    constructor(user: String, telefono: String, password: String, fingerprintId: String,correo:String, userid:String ){
        this.user=user
        this.telefono=telefono
        this.password=password
        this.fingerprintId=fingerprintId
        this.correo=correo
        this.userid=userid
    }


    override fun toString():String{
        return "$user $telefono $password"
    }
}