package com.example.primeraentrega.Clases

import java.util.Date

class Grupo(
    var descripcion: String = "",
    var titulo: String = "",
    var fotoGrupo: String = "",
    var integrantes: MutableMap<String?, String?> = mutableMapOf(),
    var planes: Map<String, Plan> = emptyMap(),
    var mensajes: Map<String, Mensaje> = emptyMap()
) {
    constructor() : this("", "", "", mutableMapOf(), emptyMap(), emptyMap())
}