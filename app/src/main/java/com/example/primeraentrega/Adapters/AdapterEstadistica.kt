package com.example.primeraentrega.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.primeraentrega.Clases.Estadistica
import com.example.primeraentrega.R

class AdapterEstadistica(context: Context, estadisticaList: MutableList<Estadistica>) : ArrayAdapter<Estadistica>(context,
    R.layout.celda_plan, estadisticaList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val estadistica = getItem(position)

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.celda_plan, parent, false)
        }

        val tv = convertView!!.findViewById<TextView>(R.id.nombreText)
        val tv2 = convertView!!.findViewById<TextView>(R.id.nombreTextFecha)

        estadistica?.let { estCelda ->
            tv.text = estCelda.nombre
            tv2.text= estCelda.pasos.toString()
        }
        return convertView
    }
}