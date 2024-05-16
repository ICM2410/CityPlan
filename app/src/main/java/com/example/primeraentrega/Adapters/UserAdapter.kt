package com.example.primeraentrega.Adapters

import com.example.primeraentrega.R.layout.contactrow
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CursorAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.primeraentrega.Clases.ListUser
import com.example.primeraentrega.R
import com.example.primeraentrega.databinding.ContactrowBinding



class UserAdapter(context: Context?, usuarioList: MutableList<ListUser>) : ArrayAdapter<ListUser>(context!!, contactrow, usuarioList) {
    // List to keep track of selected users
    private val selectedUsers: MutableList<ListUser> = mutableListOf()
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val usuario = getItem(position)

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.contactrow, parent, false)
        }

        val tvNombre = convertView!!.findViewById<TextView>(R.id.contactName)
        val ivProfilePic = convertView.findViewById<ImageView>(R.id.contactProfilePic)
        val invitarGrupo = convertView.findViewById<CheckBox>(R.id.invitarGrupo)


        usuario?.let { usuarioCelda ->
            tvNombre.text = usuarioCelda.nombre
            // Poner la imagen correspondiente
            ivProfilePic.setImageBitmap(usuarioCelda.imagen)

            // Acción al hacer clic en el botón
            invitarGrupo.setOnClickListener {
                val isChecked = invitarGrupo.isChecked

                // Realizar acciones basadas en el estado de la casilla de verificación
                if (isChecked) {
                    // If checkbox is checked, add the user to selectedUsers list
                    selectedUsers.add(usuarioCelda)
                } else {
                    // If checkbox is unchecked, remove the user from selectedUsers list
                    selectedUsers.remove(usuarioCelda)
                }
            }
        }

        return convertView
    }

    // Method to get the list of selected users
    fun getSelectedUsers(): List<ListUser> {
        return selectedUsers.toList()
    }

    fun clearSelection() {
        TODO("Not yet implemented")
    }
}