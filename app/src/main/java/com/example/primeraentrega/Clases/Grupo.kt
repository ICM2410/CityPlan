package com.example.primeraentrega.Clases

class Grupo(
    var descripcion: String = "",
    var titulo: String = "",
    var fotoGrupo: String = "",
    var integrantes: MutableMap<String?, String?> =   mutableMapOf(),
    var planes: Map<String, Plan> = emptyMap(),
){
    constructor() : this("", "", "",  mutableMapOf(),emptyMap())
}