package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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
import android.widget.Toast
import com.example.primeraentrega.Alarms.AlarmItem
import com.example.primeraentrega.Alarms.AndroidAlarmScheduler
import com.example.primeraentrega.Clases.PlanJson
import com.example.primeraentrega.Clases.PosAmigo
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.Notifications.FcmApi
import com.example.primeraentrega.Notifications.NotificationBody
import com.example.primeraentrega.Notifications.PlanState
import com.example.primeraentrega.Notifications.SendMessageDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var idGrupo : String
    private lateinit var database : FirebaseDatabase

    var imagenPin:Bitmap?=null
    private var idPlan : String=""
    private var idAlarma=0

    private lateinit var scheduler: AndroidAlarmScheduler
    var alarmItem:AlarmItem?=null
    private lateinit var auth:FirebaseAuth
    
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
        auth=FirebaseAuth.getInstance()
        val pantalla = intent.getStringExtra("pantalla")
        idGrupo=intent.getStringExtra("idGrupo").toString()
        Log.i("idGrupo","revisar Crear $idGrupo")
        database = FirebaseDatabase.getInstance()

        scheduler=AndroidAlarmScheduler(this)

        inicializarBotones()

        //de la pantalla elegir unicacion
        obtenerInformacionLocalizacion()

        databaseReference= FirebaseDatabase.getInstance().getReference("Planes")

        if (pantalla != null && "ubicacion".equals(intent.getStringExtra("pantalla"))) {
            // El extra "pantalla" existe y su valor es "crear"
            leerInfo()
        }
        else if(pantalla != null && "planes".equals(intent.getStringExtra("pantalla")))
        {
            borrarInfoJson()
        }
    }

    private fun borrarInfoJson() {
        try {
            // Obtener la ruta del archivo plan.json en el directorio de archivos externos
            val filename = "plan.json"
            val file = File(baseContext.getExternalFilesDir(null), filename)

            // Verificar si el archivo existe antes de borrarlo
            if (file.exists()) {
                // Borrar el contenido del archivo
                BufferedWriter(FileWriter(file)).use { output ->
                    output.write("") // Escribir una cadena vacía para borrar todo el contenido
                }

                Log.i("LOCATION", "File cleared at path: $file")
            } else {
                Log.i("LOCATION", "File does not exist: $file")
            }
        } catch (e: IOException) {
            Log.e("LOCATION", "Error clearing file: ${e.message}")
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
            binding.seleccionarUbicacion.setText( "seleccionar ubicacion")
        }
    }

    private fun leerInfo() {
        //leer info del json

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
                val fotopinBase64 = jsonObject.getString("fotoPinGrande")
                val fotopinByteArray = Base64.decode(fotopinBase64, Base64.DEFAULT)
                loadImage(ByteArrayInputStream(fotopinByteArray))

// Formato de fecha que incluye la hora
                val formatoFechaHora = SimpleDateFormat("M/d/yyyy HH:mm", Locale.getDefault())

// Establecer la zona horaria como UTC
                formatoFechaHora.timeZone = TimeZone.getTimeZone("UTC")

// Obtener la fecha y hora de inicio
                val dateInicio = Date(dateInMillisInicio)
                val fechaHoraInicio = formatoFechaHora.format(dateInicio)

// Separar la fecha y la hora de inicio
                val partesInicio = fechaHoraInicio.split(" ")
                val fechaInicio = partesInicio[0]
                val horaInicio = partesInicio[1]

// Mostrar la fecha y la hora de inicio en el TextView correspondiente
                binding.fechaInicio.setText(fechaInicio)
                binding.horaInicio.setText(horaInicio)

// Obtener la fecha y hora de fin
                val dateFin = Date(dateInMillisFin)
                val fechaHoraFin = formatoFechaHora.format(dateFin)

// Separar la fecha y la hora de fin
                val partesFin = fechaHoraFin.split(" ")
                val fechaFin = partesFin[0]
                val horaFin = partesFin[1]

// Mostrar la fecha y la hora de fin en el TextView correspondiente
                binding.editTextText66.setText(fechaFin)
                binding.horaFin.setText(horaFin)


                //OBTENER INFORMACION DE LA MINIS IMAGENES
                //ACA SE USARAN PARA OTRA COSA
                val MINIfotopinBase64 =jsonObject.getString("fotoPin")
                val MINIfotopinByteArray = Base64.decode(MINIfotopinBase64, Base64.DEFAULT)
                imagenPin= BitmapFactory.decodeStream(ByteArrayInputStream(MINIfotopinByteArray))
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

    private var isFabOpen=false
    private var rotation=false
    private fun inicializarBotones() {

        binding.crearplanButton.setOnClickListener {
            if("seleccionar ubicacion".equals(binding.seleccionarUbicacion.text.toString()))
            {
                Toast.makeText(this, "Debe llenar todos los datos", Toast.LENGTH_LONG ).show()
            }
            else if(!concordanciaFechas(binding.fechaInicio, binding.horaInicio,binding.editTextText66, binding.horaFin))
            {
                Toast.makeText(this, "Fechas incorrectas", Toast.LENGTH_LONG ).show()
            }
            else
            {
                var flag=0
                //editar la informacion
                guardarInformacionFirebase { documentId ->
                    //alarmId=generateUniqueCode(documentId)
                    if(flag==0)
                    {
                        ponerAlarma(documentId)
                        enviarNotificaciones(documentId)
                        flag++
                    }
                }
            }
        }

        binding.seleccionarUbicacion.setOnClickListener {
            //GUARDAR EN MEMORIA INTERNA EN UN JSON LA INFORMACION DEL PLAN
            //BORRAR LA INFORMACION DEL ARCHIVO SI ES QUE HAY
            guardarInformacion()

            val intent = Intent(baseContext, ElegirUbicacionActivity::class.java)
            intent.putExtra("idPlan", documentId)
            intent.putExtra("pantalla","crear")
            intent.putExtra("idGrupo", idGrupo)
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

        inicializarPickers()

        val usuario = UsuarioAmigo()
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

        fabClicks()
    }

    private val api: FcmApi = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create()
    private fun enviarNotificaciones(documentId: String) {
        val isBroadcast=true
        var state = PlanState(true,
            "",
            "El plan ${binding.nombrePlan.text.toString()} se ha creado y es agendado para iniciar a las ${binding.fechaInicio.text.toString()} ${binding.horaInicio.text.toString()}",
            documentId,
            0,
            idGrupo)
        val message= SendMessageDTO(
            to=if(isBroadcast) "1" else state.remoteToken,
            notification = NotificationBody(
                title = "Nuevo plan!",
                body = state.messageText,
                id = state.idPlan,
                alarmId = state.idAlarm,
                idGrupo = state.idGrupo
            )
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isBroadcast){
                    api.broadcast(message)
                } else {
                    api.sendMessage(message)
                }
            } catch (e: HttpException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        val intent = Intent(baseContext, PlanesActivity::class.java)
        intent.putExtra("idPlan", documentId)
        intent.putExtra("idGrupo", idGrupo)
        startActivity(intent)
     }

    private fun ponerAlarma(documentId: String) {

        /*alarmItem=AlarmItem(
            textoAFechaAlarma(binding.fechaInicio, binding.horaInicio),
            //textoAFechaAlarma(binding.fechaInicio, binding.horaInicio),
            "El plan ${binding.nombrePlan.text.toString()} ha iniciado",
            binding.nombrePlan.text.toString(),
            idAlarma,
            documentId,
            idGrupo
        )

        alarmItem?.let (scheduler::schedule)*/
    }

    fun textoAFechaAlarma(fechaTexto: Button, horaTexto: Button): LocalDateTime {
        // Parsear los textos de fecha y hora en LocalDateTime
        val formatter = DateTimeFormatter.ofPattern("M/d/yyyy HH:mm")

        // Parsear los textos de fecha y hora en LocalDateTime
        val fechaHora = LocalDateTime.parse("${fechaTexto.text.toString()} ${horaTexto.text.toString()}", formatter)
        Log.i("tiempo","es: $fechaHora")
        // Calcular la diferencia en segundos entre la hora actual y la fechaHora propuesta
        val diferenciaSegundos = LocalDateTime.now().until(fechaHora, java.time.temporal.ChronoUnit.SECONDS)
        Log.i("tiempo","es: diferencias local ${LocalDateTime.now()} con  inicio $diferenciaSegundos")
        // Ajustar la hora actual sumando la diferencia en segundos
        return LocalDateTime.now().plusSeconds(diferenciaSegundos)
    }

    fun generateUniqueCode(text: String): Int {
        return 1
    }

    private fun concordanciaFechas(fechaTexto1: View, horaTexto1: View, fechaTexto2: View, horaTexto2: View): Boolean {

        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Parsear la fecha y la hora de los TextView
        val fecha = formatoFecha.parse((fechaTexto1 as Button).text.toString())
        val hora = formatoHora.parse((horaTexto1 as Button).text.toString())

        // Combinar la fecha y la hora en un solo objeto Date
        val calendar = Calendar.getInstance()
        calendar.time = fecha
        val horaCalendar = Calendar.getInstance()
        horaCalendar.time = hora
        calendar.set(Calendar.HOUR_OF_DAY, horaCalendar.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, horaCalendar.get(Calendar.MINUTE))

        // Obtener la fecha y hora actual
        val fechaActual = Date()

        // Validar si la fecha y hora ingresadas son posteriores a la fecha y hora actual
        return calendar.time.after(fechaActual) && segundaEsMayorQuePrimera(fechaTexto1, horaTexto1, fechaTexto2, horaTexto2)
    }
    fun segundaEsMayorQuePrimera(fechaTexto1: View, horaTexto1: View, fechaTexto2: View, horaTexto2: View): Boolean {
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Parsear la fecha y la hora de los TextView
        val fecha1 = formatoFecha.parse((fechaTexto1 as Button).text.toString())
        val hora1 = formatoHora.parse((horaTexto1 as Button).text.toString())
        val fecha2 = formatoFecha.parse((fechaTexto2 as Button).text.toString())
        val hora2 = formatoHora.parse((horaTexto2 as Button).text.toString())

        // Combinar la fecha y la hora en un solo objeto Date
        val calendar1 = Calendar.getInstance()
        calendar1.time = fecha1
        val horaCalendar1 = Calendar.getInstance()
        horaCalendar1.time = hora1
        calendar1.set(Calendar.HOUR_OF_DAY, horaCalendar1.get(Calendar.HOUR_OF_DAY))
        calendar1.set(Calendar.MINUTE, horaCalendar1.get(Calendar.MINUTE))

        val calendar2 = Calendar.getInstance()
        calendar2.time = fecha2
        val horaCalendar2 = Calendar.getInstance()
        horaCalendar2.time = hora2
        calendar2.set(Calendar.HOUR_OF_DAY, horaCalendar2.get(Calendar.HOUR_OF_DAY))
        calendar2.set(Calendar.MINUTE, horaCalendar2.get(Calendar.MINUTE))

        // Comparar las fechas y horas
        return calendar2.time.after(calendar1.time)
    }

    private fun fabClicks() {
        binding.fabPlanesPasados.setOnClickListener {
            var intent = Intent(baseContext, PlanesPasadosActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabCrearPlan.setOnClickListener {

        }

        binding.fabMisPlanes.setOnClickListener {
            var intent = Intent(baseContext, PlanesActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabPlanActivo.setOnClickListener {
            revisarActivo()
        }
    }

    private fun revisarActivo() {
        var existe=false
        val ref = FirebaseDatabase.getInstance().getReference("Groups")
        ref.child(idGrupo).child("planes").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    // Obtiene los datos de cada usuario
                    val planId = userSnapshot.key // El ID del usuario
                    val planData = userSnapshot.getValue(Plan::class.java) // Los datos del usuario convertidos a objeto Usuario

                    // Aquí puedes realizar cualquier operación con los datos del usuario
                    println("ID de usuario: $planId")
                    println("Datos de usuario: $planData")

                    // Crea un objeto PosAmigo con la información del usuario
                    var status=""
                    val plan = planData?.let {
                        status=planAcrivo(planData.dateInicio,planData.dateFinal)
                    }

                    // Si el usuario y su ID no son nulos, añádelos al mapa integrantesMap
                    if (planId != null &&  plan != null && status!="Activo") {
                        existe=true
                        idPlan=planId
                    }
                }

                if(existe)
                {
                    var intent = Intent(baseContext, PlanActivity::class.java)
                    intent.putExtra("idGrupo", idGrupo)
                    intent.putExtra("idPlan", idPlan)
                    startActivity(intent)
                }
                else
                {
                    Toast.makeText(applicationContext, "No hay planes activos", Toast.LENGTH_LONG).show()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Maneja el error en caso de que ocurra
                println("Error al obtener los datos de planes: ${databaseError.message}")
            }
        })
    }

    private fun planAcrivo(dateInicio: java.util.Date, dateFinal: java.util.Date): String {
        val fechaActual = Date()

        return when {
            fechaActual.before(dateInicio) -> "Activo"
            fechaActual.after(dateFinal) -> "Cerrado"
            else -> "Abierto"
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

        binding.fechaInicio.setText("$month/$dayOfMonth/$year")
        binding.editTextText66.setText("$month/$dayOfMonth/$year")
        binding.horaInicio.setText("1:00")
        binding.horaFin.setText("1:00")

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

    private fun guardarInformacion() {
        //obtener el drwable a bitmap
        val bitmapPinPlanImg = (binding.pinPlanImg.drawable as BitmapDrawable).bitmap
        val streamPinPlanImgGrande = ByteArrayOutputStream()
        bitmapPinPlanImg.compress(Bitmap.CompressFormat.PNG, 100, streamPinPlanImgGrande)
        val byteArrayPinPlanImgGrande = streamPinPlanImgGrande.toByteArray()

        //hacerle resize a 60x60px
        var resizedbitmapPinPlanImg = createCircledImage(bitmapPinPlanImg)
        resizedbitmapPinPlanImg = Bitmap.createScaledBitmap(resizedbitmapPinPlanImg, 60, 60, true)
        val streamPinPlanImg = ByteArrayOutputStream()
        resizedbitmapPinPlanImg.compress(Bitmap.CompressFormat.PNG, 100, streamPinPlanImg)
        val byteArrayPinPlanImg = streamPinPlanImg.toByteArray()
        resizedbitmapPinPlanImg.recycle()

        val bitmapImagenPlan= (binding.imagenPlan.drawable as BitmapDrawable).bitmap
        val streamImagenPlanGrande = ByteArrayOutputStream()
        bitmapImagenPlan.compress(Bitmap.CompressFormat.PNG, 100, streamImagenPlanGrande)
        val byteArrayImagenPlanGrande = streamImagenPlanGrande.toByteArray()

        val myPlan = PlanJson(
            textoAFecha(binding.fechaInicio, binding.horaInicio),
            textoAFecha(binding.editTextText66, binding.horaFin),
            longitud,
            latitud,
            binding.switchPasos.isChecked,
            binding.nombrePlan.text.toString(),
            byteArrayImagenPlanGrande,
            byteArrayPinPlanImg,
            byteArrayPinPlanImgGrande
        )

        writeJSONObject(myPlan);
    }
    private fun writeJSONObject(myPlan: PlanJson) {
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

    private fun guardarInformacionFirebase(callback: (String) -> Unit) {

        val userRef = database.getReference("Groups")
        val integrantesMap = mutableMapOf<String,PosAmigo>()
        val childId = databaseReference.child("Planes").push().key.toString()
        val grupoRef = FirebaseDatabase.getInstance().getReference("Groups").child(idGrupo!!)
        //val planId = grupoRef.child("planes").push().key
        userRef.child(idGrupo).child("integrantes").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    // Obtiene los datos de cada usuario
                    val userId = userSnapshot.key // El ID del usuario
                    val userData = userSnapshot.getValue().toString() // Los datos del usuario convertidos a objeto Usuario

                    // Aquí puedes realizar cualquier operación con los datos del usuario
                    println("ID de usuario: $userId")
                    println("Datos de usuario: $userData")

                    //obtener datos del usuario
                    userId?.let {
                        val userRefID = database.getReference("Usuario").child(it)
                        userRefID.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                // Crea un objeto PosAmigo con la información del usuario
                                val usuario=dataSnapshot.getValue(UsuarioAmigo::class.java)

                                val posUsuario = userData?.let {
                                    usuario?.let { it1 -> PosAmigo(it1.latitud, usuario.longitud, usuario.uid, usuario.imagen, usuario.username) }
                                }

                                // Si el usuario y su ID no son nulos, añádelos al mapa integrantesMap
                                if (userId != null && posUsuario != null) {
                                    integrantesMap[userId] = posUsuario
                                    //de ahi se crea un grupo y se guardan ahi todos los usuarios
                                    var direccionpin = "pines/$childId-pin.png"
                                    var direccionplan = "planes/$childId-plan.png"
                                    //val pinImagesRef = storageRef.child(direccionpin)

                                    val myPlan = Plan(
                                        textoAFecha(binding.fechaInicio, binding.horaInicio),
                                        textoAFecha(binding.editTextText66, binding.horaFin),
                                        latitud,
                                        longitud,
                                        binding.switchPasos.isChecked,
                                        binding.nombrePlan.text.toString(),
                                        direccionplan,
                                        direccionpin,
                                        true,
                                        integrantesMap,
                                        childId
                                    )

                                    idAlarma=myPlan.hashCode()
                                    myPlan.idAlarma=idAlarma
                                    Log.i("childId crear","$childId")
                                    if (childId != null) {
                                        databaseReference.child(childId).setValue(myPlan).addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val drawableplan = binding.imagenPlan.drawable
                                                uploadFoto(drawableplan, direccionplan)

                                                val drawablepin = binding.pinPlanImg.drawable
                                                uploadFoto(drawablepin, direccionpin)

                                                // Guardar el nuevo plan en el mapa de planes
                                                grupoRef.child("planes").child(childId).setValue(myPlan)
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            // El plan se guardó correctamente
                                                            callback(childId)
                                                        } else {
                                                            // Hubo un error al guardar el plan
                                                            Toast.makeText(applicationContext, "Fallo en guardar la información del plan", Toast.LENGTH_LONG).show()
                                                        }
                                                    }


                                            } else {
                                                Toast.makeText(applicationContext, "Fallo en guardar la información del plan", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            }
                            override fun onCancelled(databaseError: DatabaseError) {
                                // Maneja el error en caso de que ocurra
                                println("Error al obtener los datos del usuario: ${databaseError.message}")
                            }
                        })
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Maneja el error en caso de que ocurra
                println("Error al obtener los datos del usuario: ${databaseError.message}")
            }
        })
    }

    private fun uploadFoto(drawableFoto: Drawable, nombre:String) {
        var imageUri: Uri? = null

        //var imgUrlplan: String? =null

        storageReference=FirebaseStorage.getInstance().getReference(nombre)

        if (drawableFoto != null) {
            if (drawableFoto is BitmapDrawable) {
                // Si el Drawable es un BitmapDrawable, puedes obtener el Bitmap y luego su URI
                val bitmap = drawableFoto.bitmap
                imageUri = bitmapToUri(this, bitmap)
                storageReference.putFile(imageUri).addOnSuccessListener {
                    //hideProgressBar()
                    //Toast.makeText(this, "Usuario guardado correctamente", Toast.LENGTH_LONG ).show()
                }.addOnFailureListener()
                {
                    //hideProgressBar()
                    Toast.makeText(this, "Fallo en guardar la informacion del usuario", Toast.LENGTH_LONG ).show()
                }

            }
        }
    }
    fun bitmapToUri(context: Context, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    fun textoAFecha(fechaTexto: View, horaTexto: View): Date {
        val formatoFecha = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        val fecha = formatoFecha.parse((fechaTexto as Button).text.toString())

        // Especificar la zona horaria como UTC para la hora
        formatoHora.timeZone = TimeZone.getTimeZone("UTC")
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
        var srcBitmap = BitmapFactory.decodeStream(imageStream)

        srcBitmap=createCircledImage(srcBitmap)
        //srcBitmap.recycle()

        // Establecer la imagen circular en la vista correspondiente
        if (destinationFoto == 1) {
            binding.imagenPlan.setImageBitmap(srcBitmap)
        } else {
            binding.pinPlanImg.setImageBitmap(srcBitmap)
        }
    }
    private fun createCircledImage(srcBitmap: Bitmap?): Bitmap {
        val squareBitmapWidth = min(srcBitmap!!.width, srcBitmap.height)

        // Generate a bitmap with the above value as dimensions
        val dstBitmap = Bitmap.createBitmap(
            squareBitmapWidth,
            squareBitmapWidth,
            Bitmap.Config.ARGB_8888
        )

        // Initializing a Canvas with the above generated bitmap
        val canvas = Canvas(dstBitmap)

        // initializing Paint
        val paint = Paint()
        paint.isAntiAlias = true

        // Generate a square (rectangle with all sides same)
        val rect = Rect(0, 0, squareBitmapWidth, squareBitmapWidth)
        val rectF = RectF(rect)

        // Operations to draw a circle
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val left = ((squareBitmapWidth - srcBitmap.width) / 2).toFloat()
        val top = ((squareBitmapWidth - srcBitmap.height) / 2).toFloat()
        canvas.drawBitmap(srcBitmap, left, top, paint)

        return srcBitmap;
    }
}