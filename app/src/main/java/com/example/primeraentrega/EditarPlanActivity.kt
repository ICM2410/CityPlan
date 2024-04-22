package com.example.primeraentrega

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
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
import android.graphics.drawable.VectorDrawable
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.databinding.ActivityCrearPlanBinding
import com.example.primeraentrega.databinding.ActivityEditarPlanBinding
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

class EditarPlanActivity : AppCompatActivity() {
    private lateinit var binding : ActivityEditarPlanBinding
    private var destinationFoto=1
    private lateinit var geocoder: Geocoder

    private var longitud=0.0
    private var latitud=0.0
    private var idPlan=""
    private lateinit var pantalla:String
    val db = Firebase.firestore
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference

    val getContentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback {
            if (it != null) {
                val imageStream = contentResolver.openInputStream(it)
                loadImage(imageStream)
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityEditarPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geocoder = Geocoder(baseContext)

        idPlan= intent.getStringExtra("idPlan").toString()
        Log.d(ContentValues.TAG, "ID RECIBIDO $idPlan")


        inicializarBotones()
        inicializarInformacion()
    }

    private fun inicializarInformacion() {

        val docRef = db.collection("Planes").document(idPlan)
        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d(ContentValues.TAG, "encontrado - ${documentSnapshot.id} => ${documentSnapshot.data}")
                    // Aquí puedes acceder a los datos del documento utilizando document.data
                    val plan = documentSnapshot.toObject<Plan>()

                    if (plan != null) {
                        longitud=plan.longitude
                        latitud=plan.latitude
                        binding.seleccionarUbicacion.setText( findAddress (LatLng(plan.latitude, plan.longitude)))
                        obtenerInformacionLocalizacion()
                    }

                    // Convertir fecha de milisegundos a objeto Date
                    val formatoFecha = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

                    if (plan != null) {
                        binding.switchPasos.isChecked= plan.AmigoMasActivo
                    }

                    if (plan != null) {
                        binding.nombrePlan.setText(plan.titulo)
                    }

                    //obtener imagenes
                    val pathReferencePin = plan?.let { storageRef.child(it.fotopin) }

                    val ONE_MEGABYTE: Long = 1024 * 1024
                    pathReferencePin?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                        // Los bytes de la imagen se han recuperado exitosamente
                        destinationFoto=2
                        val imageStream = ByteArrayInputStream(bytes)
                         loadImage(imageStream)
                    }?.addOnFailureListener {
                        // Manejar cualquier error que ocurra durante la recuperación de la imagen
                    }

                    val pathReferencePlan = plan?.let { storageRef.child(it.fotoEncuentro) }
                    pathReferencePlan?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                        // Los bytes de la imagen se han recuperado exitosamente
                        destinationFoto=1
                        val imageStream = ByteArrayInputStream(bytes)
                        loadImage(imageStream)
                    }?.addOnFailureListener {
                        // Manejar cualquier error que ocurra durante la recuperación de la imagen
                    }

                    val dateInicio = Date((plan?.dateInicio?.seconds ?: 0) * 1000L) // Convertir segundos a milisegundos
                    val dateFin = Date((plan?.dateFinal?.seconds ?: 0) * 1000L) // Convertir segundos a milisegundos

                    binding.fechaInicio.setText(formatoFecha.format(dateInicio))
                    binding.editTextText66.setText(formatoFecha.format(dateFin))
                    val formatoHora = SimpleDateFormat("h:mm", Locale.getDefault())
                    binding.horaInicio.setText(formatoHora.format(dateInicio))
                    binding.horaFin.setText(formatoHora.format(dateFin))

                } else {
                    Log.d(ContentValues.TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "get failed with ", exception)
            }
    }

    private fun obtenerInformacionLocalizacion() {

        //REVISAR SI SE EDITO

        // Obtener la información del Intent
        val longitudInterna= intent.getDoubleExtra("longitud", 3000.0)
        val latitudInterna= intent.getDoubleExtra("latitud", 3000.0)

        // Verificar si se recibió la información de ubicación correctamente
        if (longitudInterna !=3000.0 && latitudInterna!= 3000.0) {
            // La información de ubicación se recibió correctamente
            binding.seleccionarUbicacion.setText( findAddress (LatLng(latitudInterna, longitudInterna)))
            latitud=latitudInterna
            longitud=longitudInterna
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
        binding.editarplanButton.setOnClickListener {
            guardarInformacion { documentId ->
                val intent = Intent(baseContext, PlanActivity::class.java)
                Log.e(ContentValues.TAG, "HOLA - $idPlan")
                intent.putExtra("idPlan", idPlan)
                startActivity(intent)
            }
        }

        binding.seleccionarUbicacion.setOnClickListener {
            //GUARDAR EN MEMORIA INTERNA EN UN JSON LA INFORMACION DEL PLAN
            //BORRAR LA INFORMACION DEL ARCHIVO SI ES QUE HAY
            guardarInformacion { documentId ->
                val intent = Intent(baseContext, ElegirUbicacionActivity::class.java)
                intent.putExtra("idPlan", idPlan)
                intent.putExtra("pantalla","editar")
                startActivity(intent)
            }
        }

        binding.imagenPlan.setOnClickListener{
            destinationFoto=1
            getContentGallery.launch("image/*")

        }

        binding.pinPlanImg.setOnClickListener{
            destinationFoto=2
            getContentGallery.launch("image/*")
        }

        inicializarPickers()
    }



    private fun inicializarPickers() {
        binding.fechaInicio.setOnClickListener {
            openDateDialogue(binding.fechaInicio.context, binding.fechaInicio)
        }

        binding.editTextText66.setOnClickListener{
            openDateDialogue(binding.editTextText66.context, binding.editTextText66)
        }

        binding.horaInicio.setOnClickListener {
            openTimeDialogue(binding.horaInicio.context,binding.horaInicio)
        }

        binding.horaFin.setOnClickListener {
            openTimeDialogue(binding.horaFin.context,binding.horaFin)
        }
    }

    fun openDateDialogue(context: Context, view: View) {
        // Aquí irá el código para mostrar el diálogo de selección de fecha
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            context,
            DatePickerDialog.OnDateSetListener { datePicker: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                (view as Button).text = "${month + 1}/${dayOfMonth}/${year}" // Sumo 1 al mes porque en Kotlin los meses van de 0 a 11
            },
            year,
            month,
            dayOfMonth
        )

        dialog.show()
    }

    fun openTimeDialogue(context: Context, view: View) {
        // Aquí irá el código para mostrar el diálogo de selección de fecha
        // Aquí irá el código para mostrar el diálogo de selección de tiempo
        val hourOfDay = 15
        val minute = 0

        val dialog = TimePickerDialog(
            context,
            TimePickerDialog.OnTimeSetListener { timePicker: TimePicker, hourOfDay: Int, minute: Int ->
                (view as Button).text  = "${hourOfDay}:${minute}"
            },
            hourOfDay,
            minute,
            true // Indica si se muestra el formato de 24 horas
        )

        dialog.show()
    }

    private fun guardarInformacion(callback: (String) -> Unit) {

        val drawablepin = binding.pinPlanImg.drawable
        var imageUri: Uri? = null

        //var imgUrlpin: String? =null
        var direccionpin="images/pin.png"
        val pinImagesRef = storageRef.child(direccionpin)

        if (drawablepin != null) {
            if (drawablepin is BitmapDrawable) {
                // Si el Drawable es un BitmapDrawable, puedes obtener el Bitmap y luego su URI
                val bitmap = drawablepin.bitmap
                imageUri = bitmapToUri(this, bitmap)
                pinImagesRef.putFile(imageUri).addOnSuccessListener { task ->
                    //imgUrlpin = task.getMetadata()?.getReference()?.getDownloadUrl().toString()
                }
            } else if (drawablepin is VectorDrawable || drawablepin is VectorDrawableCompat) {
                // Si el Drawable es un VectorDrawable, no se puede convertir directamente a URI
                // Puedes hacer algo aquí para manejar este caso si es necesario
            }
        }

        var direccionplan="images/plan.png"
        val planImagesRef = storageRef.child(direccionplan)
        val drawableplan = binding.imagenPlan.drawable
        //var imgUrlplan: String? =null

        if (drawableplan != null) {
            if (drawableplan is BitmapDrawable) {
                // Si el Drawable es un BitmapDrawable, puedes obtener el Bitmap y luego su URI
                val bitmap = drawableplan.bitmap
                imageUri = bitmapToUri(this, bitmap)
                planImagesRef.putFile(imageUri).addOnSuccessListener { task ->
                    pinImagesRef.downloadUrl.addOnSuccessListener { uri ->
                        // Uri es la URL de la imagen subida en Firebase Storage
                        //imgUrlplan= uri.toString()
                        // Una vez que se haya guardado la información, llamamos a la función de devolución de llamada
                        callback.invoke(idPlan)
                    }
                }
            } else if (drawableplan is VectorDrawable || drawableplan is VectorDrawableCompat) {
                // Si el Drawable es un VectorDrawable, no se puede convertir directamente a URI
                // Puedes hacer algo aquí para manejar este caso si es necesario
            }
        }

        val myPlan = Plan(
            textoAFecha(binding.fechaInicio, binding.horaInicio),
            textoAFecha(binding.editTextText66, binding.horaFin),
            latitud,
            longitud,
            binding.switchPasos.isChecked,
            binding.nombrePlan.text.toString(),
            direccionplan,
            direccionpin
        )

        val plan= hashMapOf(
            "dateInicio" to myPlan.dateInicio,
            "dateFinal" to myPlan.dateFinal,
            "latitude" to myPlan.latitude,
            "longitude" to myPlan.longitude,
            "AmigoMasActivo" to myPlan.AmigoMasActivo,
            "titulo" to myPlan.titulo,
            "fotoEncuentro" to myPlan.fotoEncuentro,
            "fotopin" to myPlan.fotopin,
        )

        val planMap: Map<String, Any> = plan.toMap()
        db.collection("Planes").document(idPlan)
            .set(plan, SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "Document successfully updated!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating document", e) }
    }

    fun bitmapToUri(context: Context, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }
    fun textoAFecha(fechaTexto: View, horaTexto: View): Date {
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        val fecha = formatoFecha.parse((fechaTexto as Button).text.toString())
        val hora = formatoHora.parse((horaTexto as Button).text.toString())

        val calendario = Calendar.getInstance()
        fecha?.let { calendario.time = it }
        hora?.let {
            calendario.set(Calendar.HOUR_OF_DAY, it.hours)
            calendario.set(Calendar.MINUTE, it.minutes)
        }
        return calendario.time

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