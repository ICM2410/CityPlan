package com.example.primeraentrega.usuario

class usuario {
    lateinit var user:String
    lateinit var telefono:String
    lateinit var password:String

    constructor()

    constructor(user:String, telefono:String,password:String){
        this.user=user
        this.telefono=telefono
        this.password=password
    }


    override fun toString():String{
        return "$user $telefono $password"
    }
}