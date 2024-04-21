package com.example.primeraentrega.usuario

import java.io.Serializable

class usuario : Serializable{
    lateinit var user:String
    lateinit var telefono:String
    lateinit var password:String
    var fingerprintId: String? = null

    constructor()

    constructor(user:String, telefono:String,password:String){
        this.user=user
        this.telefono=telefono
        this.password=password
    }

    constructor(user: String, telefono: String, password: String, fingerprintId: String){
        this.user=user
        this.telefono=telefono
        this.password=password
        this.fingerprintId=fingerprintId
    }


    override fun toString():String{
        return "$user $telefono $password"
    }
}