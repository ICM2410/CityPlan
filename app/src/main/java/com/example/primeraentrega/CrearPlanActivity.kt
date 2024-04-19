package com.example.primeraentrega

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.databinding.ActivityCrearPlanBinding
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.util.Date
import kotlin.math.min
import android.util.Base64

class CrearPlanActivity : AppCompatActivity() {

    private lateinit var binding : ActivityCrearPlanBinding
    private var destinationFoto=1
    private lateinit var geocoder: Geocoder
    private var longitud=0.0
    private var latitud=0.0

    val getContentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback {
            if (it != null) {
                val imageStream = contentResolver.openInputStream(it)
                loadImage(imageStream)
            }
        })

    private lateinit var pantalla:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityCrearPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geocoder = Geocoder(baseContext)

        inicializarBotones()
        obtenerInformacionLocalizacion()
        //OBTENER INFORMACION GUARDADA EN MEMORIA
        //EVALUAR SI EXISTE ESA INFORMACION EN MEMORIA
        leerJSON()
    }

    private fun obtenerInformacionLocalizacion() {

        // Obtener la información del Intent
        longitud= intent.getDoubleExtra("longitud", 3000.0)
        latitud= intent.getDoubleExtra("latitud", 3000.0)
        val byteArray = intent.getByteArrayExtra("pinImage")

        if (byteArray != null) {
            val imageStream = ByteArrayInputStream(byteArray)
            destinationFoto=2
            loadImage(imageStream)
        }
        // Verificar si se recibió la información de ubicación correctamente
        if (longitud !=3000.0 && latitud != 3000.0) {
            // La información de ubicación se recibió correctamente
            binding.seleccionarUbicacion.setText( findAddress (LatLng(latitud, longitud)))
        } else {
            // No se recibió la información de ubicación
        }
    }

    private fun leerJSON() {
        try {
            val file = File(getExternalFilesDir(null), "plan.json")
            val json_string = file.bufferedReader().use {
                it.readText()
            }

            val planJsonArray = JSONArray(json_string)

                       for (i in 0 until planJsonArray.length()) {

                           val jsonObject = planJsonArray.getJSONObject(i)
                           val dateInMillisInicio = jsonObject.getLong("dateInicio")
                           val dateInMillisFin = jsonObject.getLong("dateFinal")

                           val latitude = jsonObject.getDouble("latitude")
                           val longitude = jsonObject.getDouble("longitude")

                           if(latitud==3000.0)
                           {
                               latitud=latitude
                               longitud=longitude
                               binding.seleccionarUbicacion.setText( findAddress (LatLng(latitude, longitude)))
                           }

                           binding.switchPasos.isChecked= jsonObject.getBoolean("AmigoMasActivo")

                           binding.nombrePlan.setText(jsonObject.getString("titulo"))

                           destinationFoto=1
                           // Decodificar la imagen de encuentro
                           val fotoEncuentroBase64 = jsonObject.getString("fotoEncuentro")
                           val fotoEncuentroByteArray = Base64.decode(fotoEncuentroBase64, Base64.DEFAULT)
                           loadImage(ByteArrayInputStream(fotoEncuentroByteArray))

                           destinationFoto=2
                           // Decodificar la imagen del pin
                           val fotopinBase64 = jsonObject.getString("fotopin")
                           val fotopinByteArray = Base64.decode(fotopinBase64, Base64.DEFAULT)
                           loadImage(ByteArrayInputStream(fotopinByteArray))

                           // Convertir fecha de milisegundos a objeto Date
                           val dateInicio = Date(dateInMillisInicio)
                           binding.fechaInicio.setText(dateInicio.toString())
                           val dateFin = Date(dateInMillisFin)
                           binding.editTextText66.setText(dateFin.toString())
                       }

        } catch (e: IOException) {
            Log.e("LOCATION", "Error al leer el archivo JSON: ${e.message}")
        } catch (e: JSONException) {
            Log.e("LOCATION", "Error al analizar el archivo JSON: ${e.message}")
        }
    }

    fun findAddress (location : LatLng):String?{
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 2)
        if(addresses != null && !addresses.isEmpty()){
            val addr = addresses.get(0)
            val locname = addr.getAddressLine(0)
            return locname
        }
        return null
    }

    private fun inicializarBotones() {

        binding.crearplanButton.setOnClickListener {
            guardarInformacion()
            startActivity(Intent(baseContext, PlanesActivity::class.java))
            //ENVIAR A FIRE BASE EL NUEVO PLAN CREADO
            //EVALUAR SI TODA LA INFORMACION ESTA COMPLETA
        }

        binding.seleccionarUbicacion.setOnClickListener {
            //GUARDAR EN MEMORIA INTERNA EN UN JSON LA INFORMACION DEL PLAN
            //BORRAR LA INFORMACION DEL ARCHIVO SI ES QUE HAY
            guardarInformacion()

            val bitmap = (binding.pinPlanImg.drawable as BitmapDrawable).bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            val intent = Intent(baseContext, ElegirUbicacionActivity::class.java)
            intent.putExtra("pinImage", byteArray)
            intent.putExtra("pantalla","crear")
            startActivity(intent)
        }

        binding.imagenPlan.setOnClickListener{
                destinationFoto=1
                getContentGallery.launch("image/*")
        }

        binding.pinPlanImg.setOnClickListener{
            destinationFoto=2
            getContentGallery.launch("image/*")
        }
    }
    private fun guardarInformacion() {
        //GUARDAR EN MEMORIA INTERNA EN UN JSON LA INFORMACION DEL PLAN
        //BORRAR LA INFORMACION DEL ARCHIVO SI ES QUE HAY
        val bitmapPinPlanImg = (binding.pinPlanImg.drawable as BitmapDrawable).bitmap
        val streamPinPlanImg = ByteArrayOutputStream()
        bitmapPinPlanImg.compress(Bitmap.CompressFormat.PNG, 100, streamPinPlanImg)
        val byteArrayPinPlanImg = streamPinPlanImg.toByteArray()

        val bitmapImagenPlan= (binding.imagenPlan.drawable as BitmapDrawable).bitmap
        val streamImagenPlan = ByteArrayOutputStream()
        bitmapImagenPlan.compress(Bitmap.CompressFormat.PNG, 100, streamImagenPlan)
        val byteArrayImagenPlan = streamImagenPlan.toByteArray()

        val myPlan = Plan(
            Date(System.currentTimeMillis()),
            Date(System.currentTimeMillis()),
            longitud,
            latitud,
            binding.switchPasos.isChecked,
            binding.nombrePlan.text.toString(),
            byteArrayImagenPlan,
            byteArrayPinPlanImg
        )

        writeJSONObject(myPlan);
    }

    private fun writeJSONObject(myPlan: Plan) {
        val plan = mutableListOf<JSONObject>()

        try {
            // Agregar la nueva ubicación a la lista de ubicaciones
            plan.add(myPlan.toJSON())

            // Convertir la lista de objetos JSON a una cadena JSON
            val jsonArray = JSONArray(plan)

            // Obtener la ruta del archivo locations.json en el directorio de archivos externos
            val filename = "plan.json"
            val file = File(baseContext.getExternalFilesDir(null), filename)

            // Escribir la cadena JSON en el archivo
            BufferedWriter(FileWriter(file)).use { output ->
                output.write(jsonArray.toString())
            }

            Log.i("LOCATION", "File modified at path: $file")
        } catch (e: IOException) {
            Log.e("LOCATION", "Error writing to file: ${e.message}")
        }
    }

    private fun loadImage(imageStream:  InputStream?) {
        val originalBitmap = BitmapFactory.decodeStream(imageStream)

        // Crear un bitmap cuadrado con el tamaño máximo entre el ancho y el alto de la imagen
        val squareBitmap = Bitmap.createBitmap(
            min(originalBitmap.width, originalBitmap.height),
            min(originalBitmap.width, originalBitmap.height),
            Bitmap.Config.ARGB_8888
        )

        // Crear un lienzo para dibujar en el bitmap cuadrado
        val canvas = Canvas(squareBitmap)

        // Dibujar la imagen original en el centro del bitmap cuadrado
        val left = (squareBitmap.width - originalBitmap.width) / 2f
        val top = (squareBitmap.height - originalBitmap.height) / 2f
        canvas.drawBitmap(originalBitmap, left, top, null)

        // Crear un bitmap circular
        val circleBitmap = Bitmap.createBitmap(
            squareBitmap.width,
            squareBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(squareBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val rect = Rect(0, 0, squareBitmap.width, squareBitmap.height)
        val rectF = RectF(rect)
        val diameter = min(squareBitmap.width, squareBitmap.height).toFloat()
        canvas.setBitmap(circleBitmap)
        canvas.drawCircle(diameter / 2, diameter / 2, diameter / 2, paint)

        // Establecer la imagen circular en la vista correspondiente
        if (destinationFoto == 1) {
            binding.imagenPlan.setImageBitmap(circleBitmap)
        } else {
            binding.pinPlanImg.setImageBitmap(circleBitmap)
        }
    }
}