package com.example.primeraentrega.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.primeraentrega.Clases.ListMessage
import com.example.primeraentrega.Clases.Mensaje
import com.example.primeraentrega.R
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class ChatAdapter (private val context: Context, private val messages: List<ListMessage>, private val currentUserUid: String) : ArrayAdapter<ListMessage>(context, 0, messages) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val message = messages[position]

        // Inflate the appropriate layout based on the sender of the message
        if (message.emisorUID == currentUserUid) {
            //Set Mensaje
            view = LayoutInflater.from(context).inflate(R.layout.chat_usuario_actual, parent, false)
            val mensajeTextView: TextView = view.findViewById(R.id.text_gchat_message_me)
            mensajeTextView.text = message.mensaje
            //Set date
            val dateTextView: TextView = view.findViewById(R.id.text_gchat_date_me)
            val calendar = Calendar.getInstance()
            val date = Date(message.createdAt)
            calendar.time = date

            val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            dateTextView.text = "%02d/%02d".format(month, dayOfMonth)
            //Set time
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timeTextView: TextView = view.findViewById(R.id.text_gchat_timestamp_me)
            timeTextView.text = "%02d:%02d".format(hourOfDay, minute)






        } else {
            view = LayoutInflater.from(context).inflate(R.layout.chat_usuario_externo, parent, false)

            // Set the profile image for the external user
            val profileImage = view.findViewById<ImageView>(R.id.image_gchat_profile_other)
            profileImage.setImageBitmap(message.imagenUsuario)

            //Set sender
            val emisorTextView: TextView = view.findViewById(R.id.text_gchat_user_other)
            emisorTextView.text = message.emisor
            //Set message
            val messageTextView: TextView = view.findViewById(R.id.text_gchat_message_other)
            messageTextView.text = message.mensaje


            //Set date
            val dateTextView: TextView = view.findViewById(R.id.text_gchat_date_other)
            val calendar = Calendar.getInstance()
            val date = Date(message.createdAt)
            calendar.time = date

            val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            dateTextView.text = "%02d/%02d".format(month, dayOfMonth)

            //Set time
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timeTextView: TextView = view.findViewById(R.id.text_gchat_timestamp_other)
            timeTextView.text = "%02d:%02d".format(hourOfDay, minute)

        }


        return view
    }
}
