package com.example.primeraentrega.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.primeraentrega.Clases.Establecimiento
import com.example.primeraentrega.R.layout.celda_recomendacion
import com.example.primeraentrega.R
import com.squareup.picasso.Picasso

class AdapterEstablecimiento (context: Context, establecimientoList: MutableList<Establecimiento>) : ArrayAdapter<Establecimiento>(context, celda_recomendacion, establecimientoList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val pais = getItem(position)

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.celda_recomendacion, parent, false)
        }

        val tv = convertView!!.findViewById<TextView>(R.id.nombrePais)
        val tv2 = convertView!!.findViewById<TextView>(R.id.direccion)
        val iv = convertView.findViewById<ImageView>(R.id.banderaPais)

        pais?.let {
            tv.text = it.getNombre()
            tv2.text=it.getDireccion()
            Picasso.get().load(it.getImagen()).resize(80, 80).into(iv)
        }

        return convertView
    }
}