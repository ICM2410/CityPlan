package com.example.primeraentrega.Clases

import java.util.Date

class Grupo(
    var id: String = "",
    var descripcion: String = "",
    var titulo: String = "",
    var fotoGrupo: String = "",
    var integrantes: List<UsuarioAmigo> = listOf(),
    var planes: List<String> = listOf(),
)