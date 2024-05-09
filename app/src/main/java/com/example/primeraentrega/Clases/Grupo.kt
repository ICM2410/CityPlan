package com.example.primeraentrega.Clases

import java.util.Date

class Grupo(
    var descripcion: String = "",
    var titulo: String = "",
    var fotoGrupo: String = "",
    var integrantes: List<Usuario> = listOf(),
    var planes: List<String> = listOf(),
){
    constructor() : this("", "", "",  listOf(),listOf())
}