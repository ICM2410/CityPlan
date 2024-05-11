package com.example.primeraentrega.Clases

import java.util.Date

class Grupo(
    var descripcion: String = "",
    var titulo: String = "",
    var fotoGrupo: String = "",
    var integrantes: Map<String, Usuario> = emptyMap(),
    var planes: Map<String, Plan> = emptyMap(),
    var mensajes: List<Mensaje> = emptyList()
){
    constructor() : this("", "", "", emptyMap(),emptyMap())
}