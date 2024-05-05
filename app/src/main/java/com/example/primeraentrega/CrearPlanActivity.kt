package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.databinding.ActivityCrearPlanBinding
import com.google.android.gms.maps.model.LatLng
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Date
import kotlin.math.min
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.example.primeraentrega.usuario.Usuario
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CrearPlanActivity : AppCompatActivity() {

    private lateinit var binding : ActivityCrearPlanBinding
    private var destinationFoto=1
    private lateinit var geocoder: Geocoder
    private var longitud=0.0
    private var latitud=0.0
    private var documentId=""
    private var pantalla=null
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
        binding= ActivityCrearPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        geocoder = Geocoder(baseContext)

        val pantalla = intent.getStringExtra("pantalla")

        inicializarBotones()

        //de la pantalla elegir unicacion
        obtenerInformacionLocalizacion()

        if (pantalla != null && "ubicacion".equals(intent.getStringExtra("pantalla"))) {
            // El extra "pantalla" existe y su valor es "crear"
            leerInfo()
        }
    }

    private fun obtenerInformacionLocalizacion() {

        // Obtener la información del Intent
        longitud= intent.getDoubleExtra("longitud", 3000.0)
        latitud= intent.getDoubleExtra("latitud", 3000.0)

        if (longitud !=3000.0 && latitud != 3000.0) {
            // La información de ubicación se recibió correctamente
            binding.seleccionarUbicacion.setText( findAddress (LatLng(latitud, longitud)))
        } else {
            // No se recibió la información de ubicación
        }
    }

    private fun leerInfo() {

        documentId= intent.getStringExtra("idPlan").toString()
        Log.d(TAG, "entreee $documentId")

        val docRef = db.collection("Planes").document(documentId)
        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d(TAG, "encontrado - ${documentSnapshot.id} => ${documentSnapshot.data}")
                    // Aquí puedes acceder a los datos del documento utilizando document.data
                    val plan = documentSnapshot.toObject<Plan>()

                    if(latitud==3000.0)
                    {
                        if (plan != null) {
                            binding.seleccionarUbicacion.setText( findAddress (LatLng(plan.latitude, plan.longitude)))
                        }
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

                    Log.e(TAG, "DATES ${plan?.dateInicio} Y ${plan?.dateFinal}?")
                    val dateInicio = plan?.dateInicio
                    val dateFin = plan?.dateFinal

                    // Asegúrate de que las fechas no sean nulas antes de continuar
                    if (dateInicio != null && dateFin != null) {
                        // Configura el formato de fecha y hora
                        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val formatoHora = SimpleDateFormat("h:mm a", Locale.getDefault())

                        // Establece la zona horaria a UTC si es necesario
                        formatoFecha.timeZone = TimeZone.getTimeZone("UTC")
                        formatoHora.timeZone = TimeZone.getTimeZone("UTC")

                        // Configura las fechas en las vistas
                        binding.fechaInicio.setText(formatoFecha.format(dateInicio))
                        binding.editTextText66.setText(formatoFecha.format(dateFin))
                        binding.horaInicio.setText(formatoHora.format(dateInicio))
                        binding.horaFin.setText(formatoHora.format(dateFin))
                    }


                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
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

    private var isFabOpen=false
    private var rotation=false
    private fun inicializarBotones() {

        binding.crearplanButton.setOnClickListener {
                //editar la informacion
                editarInformacion { documentId ->
                    val intent = Intent(baseContext, PlanesActivity::class.java)
                    intent.putExtra("idPlan", documentId)
                    startActivity(intent)
                }

            //ENVIAR A FIRE BASE EL NUEVO PLAN CREADO
            //EVALUAR SI TODA LA INFORMACION ESTA COMPLETA
        }

        binding.seleccionarUbicacion.setOnClickListener {
            //GUARDAR EN MEMORIA INTERNA EN UN JSON LA INFORMACION DEL PLAN
            //BORRAR LA INFORMACION DEL ARCHIVO SI ES QUE HAY
            guardarInformacion { documentId ->
                val intent = Intent(baseContext, ElegirUbicacionActivity::class.java)
                intent.putExtra("idPlan", documentId)
                intent.putExtra("pantalla","crear")
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

        val usuario: Usuario = Usuario()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    startActivity(Intent(baseContext, VerGruposActivity::class.java))

                    true
                }
                R.id.cuenta_bar -> {
                    // Respond to navigation item 2 click
                    var intent = Intent(baseContext, PerfilConfActivity::class.java)
                    intent.putExtra("user", usuario)
                    startActivity(intent)
                    true
                }
                R.id.salir_bar -> {
                    // Respond to navigation item 3 click
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                    true
                }
                else -> false
            }
        }

        initShowout(binding.pasadosView)
        initShowout(binding.nuevoView)
        initShowout(binding.activoView)
        initShowout(binding.planesView)
        binding.fabMenuPlan.setOnClickListener {
            if(!isFabOpen)
            {
                showFabMenu();
            }
            else
            {
                closeFabMenu();
            }
        }
    }

    private fun initShowout (v: View){
        v.apply {
            visibility = View. GONE
            translationY= height.toFloat()
            alpha = 0f
        }
    }

    private fun closeFabMenu() {
        rotation=rotateFAB()
        isFabOpen=false
        cerrar(binding.pasadosView)
        cerrar(binding.nuevoView)
        cerrar(binding.activoView)
        cerrar(binding.planesView)
    }

    private fun cerrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=1f
            translationY=0f
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        visibility= View.GONE
                    }
                })
                .alpha(0f)
                .start()
        }
    }

    private fun showFabMenu() {
        rotation=rotateFAB()
        isFabOpen=true

        //motrar info
        mostrar(binding.pasadosView)
        mostrar(binding.nuevoView)
        mostrar(binding.activoView)
        mostrar(binding.planesView)

    }

    private fun mostrar(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=0f
            translationY=height.toFloat()
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){})
                .alpha(1f)
                .start()
        }
    }

    private fun rotateFAB():Boolean {
        binding.fabMenuPlan.animate()
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter(){})
            .rotation(if(!isFabOpen) 180f else 0f)

        return isFabOpen
    }

    private fun inicializarPickers() {

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        binding.fechaInicio.setText("$dayOfMonth/$month/$year")
        binding.editTextText66.setText("$dayOfMonth/$month/$year")
        binding.horaInicio.setText("0:00")
        binding.horaFin.setText("0:00")

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

    fun openTimeDialogue(context: Context,view: View) {
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

    // Modifica la función guardarInformacion() para que acepte una función de devolución de llamada
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
                        callback.invoke(documentId)
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

        db.collection("Planes")
            .add(plan)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                documentId=documentReference.id
                // Una vez que se haya guardado la información, llamamos a la función de devolución de llamada
                callback.invoke(documentId)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }

    }

    private fun editarInformacion(callback: (String) -> Unit) {

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
                        callback.invoke(documentId)
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
        db.collection("Planes").document(documentId)
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
            val horaCalendario = Calendar.getInstance().apply { time = it }
            calendario.set(Calendar.HOUR_OF_DAY, horaCalendario.get(Calendar.HOUR_OF_DAY))
            calendario.set(Calendar.MINUTE, horaCalendario.get(Calendar.MINUTE))
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