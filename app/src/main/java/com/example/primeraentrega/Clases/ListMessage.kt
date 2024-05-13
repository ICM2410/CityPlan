package com.example.primeraentrega.Clases

import android.graphics.Bitmap

data class ListMessage (
    var uid: String?,
    var imagenUsuario: Bitmap,
    var mensaje: String = "",
    var emisorUID: String = "",
    var emisor: String = "",
    var createdAt: Long = 0
){}