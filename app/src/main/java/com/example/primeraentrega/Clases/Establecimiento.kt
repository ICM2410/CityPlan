package com.example.primeraentrega.Clases

class Establecimiento {
    private var nombre: String
    private var imagen: String

    constructor(nombre: String, imagen: String) {
        this.nombre = nombre
        this.imagen= imagen
    }


    public fun setNombre(nombre: String) {
        this.nombre = nombre
    }

    public fun getNombre():String {
        return this.nombre
    }

    public fun setImagen(nombre: String) {
        this.imagen = nombre
    }

    public fun getImagen():String {
        return this.imagen
    }

}