package com.example.primeraentrega.Adapters

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.primeraentrega.Clases.PlanLista
import com.example.primeraentrega.R
import com.example.primeraentrega.R.layout.celda_plan
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch

class Adapterplan(context: Context, usuarioList: MutableList<PlanLista>) : ArrayAdapter<PlanLista>(context, celda_plan, usuarioList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val plan = getItem(position)

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.celda_plan, parent, false)
        }

        val tv = convertView!!.findViewById<TextView>(R.id.nombreText)
        val tv2 = convertView!!.findViewById<TextView>(R.id.nombreTextFecha)
        val iv = convertView.findViewById<ImageView>(R.id.imagePerfiles)
        val layoutPlan = convertView.findViewById<View>(R.id.layoutPlan)

        plan?.let { planCelda ->
            tv.text = planCelda.titulo

            val formatoFecha = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val formatoHora = SimpleDateFormat("h:mm a", Locale.getDefault())

            // Establece la zona horaria a UTC si es necesario
            formatoFecha.timeZone = TimeZone.getTimeZone("UTC")
            formatoHora.timeZone = TimeZone.getTimeZone("UTC")

            // Configura las fechas en las vistas

            tv2.text= formatoFecha.format(planCelda.dateInicio) +" "+formatoHora.format(planCelda.dateInicio)

            if(planCelda.actual=="Abierto")
            {
                layoutPlan.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.rosaClaro))
            }


            // Poner la imagen correspondiente
            val localfile = File. createTempFile( "tempImage", "jpg")
            //val latch = CountDownLatch(1)

            var storageRef = FirebaseStorage.getInstance().reference.child(planCelda.fotoEncuentro)

            storageRef.getFile(localfile).addOnSuccessListener {

                var src = BitmapFactory.decodeFile(localfile.absolutePath)
                iv.setImageBitmap(src)
                //latch.countDown()
                //clickLista()
            }.addOnFailureListener{
                Log.i("revisar", "no se pudo poner la foto del pin")
                //latch.countDown()
            }

           /* try {
                // Esperar a que la descarga de la imagen se complete
                latch.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }*/
        }

        return convertView
    }
}