package com.example.primeraentrega.Clases

class Establecimiento {
    private var nombre: String
    private var imagen: String
    private var latitude: Double
    private var longitude: Double
    private var direccion: String

    constructor(nombre: String, imagen: String, latitude: Double, longitude: Double, direccion:String) {
        this.nombre = nombre
        this.imagen= imagen
        this.latitude=latitude
        this.longitude=longitude
        this.direccion=direccion
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

    public fun setLatitude(latitude: Double) {
        this.latitude = latitude
    }

    public fun getLatitude():Double {
        return this.latitude
    }

    public fun setLongitude(longitude: Double) {
        this.longitude = latitude
    }

    public fun getLongitude():Double {
        return this.longitude
    }

    public fun setDireccion(nombre: String) {
        this.direccion = nombre
    }

    public fun getDireccion():String {
        return this.direccion
    }
}