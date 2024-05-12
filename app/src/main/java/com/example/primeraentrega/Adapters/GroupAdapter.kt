package com.example.primeraentrega.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.example.primeraentrega.Clases.ListGroup
import com.example.primeraentrega.Clases.ListUser
import com.example.primeraentrega.R

class GroupAdapter(context: Context?, grupoList: MutableList<ListGroup>) : ArrayAdapter<ListGroup>(context!!, R.layout.celda_generica, grupoList){
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val grupo = getItem(position)

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.celda_generica, parent, false)
        }

        val tvNombre = convertView!!.findViewById<TextView>(R.id.nombreText)
        val ivProfilePic = convertView.findViewById<ImageView>(R.id.imagePerfiles)



        grupo?.let { grupocelda ->
            tvNombre.text = grupocelda.nombre
            // Poner la imagen correspondiente
            ivProfilePic.setImageBitmap(grupocelda.imagen)

        }

        return convertView
    }
}