package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.databinding.ActivityElegirUbicacionBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONArray
import org.json.JSONException
import org.osmdroid.util.GeoPoint
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min

class ElegirUbicacionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding : ActivityElegirUbicacionBinding

    //PARA POSICIONES
    private var posActualGEO = GeoPoint(4.0, 72.0)
    private var latActual:Double= 4.0
    private var longActual:Double= 72.0
    private var latMia:Double= 4.0
    private var longMia:Double= 72.0
    private var isFabOpen=false
    private var rotation=false

    private val localPermissionName=android.Manifest.permission.ACCESS_FINE_LOCATION;
    private var MarkerActual: com.google.android.gms.maps.model.Marker? = null
    lateinit var location: FusedLocationProviderClient

    //MAPA
    private lateinit var mMap: GoogleMap
    private lateinit var geocoder: Geocoder

    //LOCALIZACION
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback

    var imagenPin:Bitmap?=null
    private lateinit var idGrupo : String

    //Sensores
    private lateinit var sensorManager: SensorManager
    //SENSOR luz
    private lateinit var lightSensor : Sensor
    private lateinit var lightEventListener: SensorEventListener


    private var firstTime=true


    val permissionRequest= registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            locationSettings()
        })

    fun locationSettings()
    {
        val builder= LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
        }
        task.addOnFailureListener{
            if(it is ResolvableApiException)
            {
                try{
                    val isr: IntentSenderRequest = IntentSenderRequest.Builder(it.resolution).build()
                    locationSettings.launch(isr)
                }
                catch (sendEx: IntentSender.SendIntentException)
                {
                    //ignore the error
                }

            }
            else
            {
                Toast.makeText(getApplicationContext(), "there is no gps hardware", Toast.LENGTH_LONG).show();
            }
        }
    }

    val locationSettings= registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ActivityResultCallback {
            if(it.
                resultCode ==
                RESULT_OK){

                startLocationUpdates()
            }else{
                Toast.makeText(getApplicationContext(), "GPS TURNED OFF", Toast.LENGTH_LONG).show();
            }
        })

    fun gestionarPermiso() {
        if(ActivityCompat.checkSelfPermission(this, localPermissionName)== PackageManager.PERMISSION_DENIED)
        {
            if(shouldShowRequestPermissionRationale(localPermissionName))
            {
                Toast.makeText(getApplicationContext(), "The app requires access to location", Toast.LENGTH_LONG).show();
            }
            permissionRequest.launch(localPermissionName)
        }
        else
        {
            startLocationUpdates()
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                localPermissionName
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            location.requestLocationUpdates(
                locationRequest,
                locationCallBack,
                Looper.getMainLooper()
            )
        } else {
            Toast.makeText(getApplicationContext(), "NO HAY PERMISO", Toast.LENGTH_LONG).show();
        }
    }
    private  fun createLocationCallback():LocationCallback
    {
        val locationCallback=object: LocationCallback()//clase anonima en kotlin
        //heredar y sobreescribir sobre la misma linea
        {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val last=result.lastLocation
                if(last!=null)
                {
                        latMia=last.latitude
                        longMia=last.longitude
                        if("recomendacion".equals(intent.getStringExtra("recomendacion").toString()))
                        {
                            Log.i("entre","entre desde recomendaciones")
                            //poner la posicion
                            firstTime=false
                            longActual= intent.getDoubleExtra("longitud", 0.0)
                            latActual= intent.getDoubleExtra("latitud", 0.0)
                            var pos=LatLng(latActual,longActual)
                            MarkerActual?.remove()
                            MarkerActual=mMap.addMarker(
                                MarkerOptions()
                                    .position(pos)
                                    .icon(imagenPin?.let { it1 -> BitmapDescriptorFactory.fromBitmap(it1) })
                                    .title("Pos seleccionada"))
                            //.icon(BitmapDescriptorFactory.fromBitmap(mifoto))
                            val zoomLevel = 15.0f // Puedes ajustar este valor según sea necesario
                            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
                            mMap.moveCamera(cameraUpdate)
                        }

                        if(firstTime)
                        {
                            firstTime=false
                            latActual=last.latitude
                            longActual=last.longitude
                            var pos=LatLng(latActual,longActual)
                            MarkerActual?.remove()
                            MarkerActual=mMap.addMarker(
                                MarkerOptions()
                                .position(pos)
                                .icon(imagenPin?.let { it1 -> BitmapDescriptorFactory.fromBitmap(it1) })
                                .title("Pos seleccionada"))
                                //.icon(BitmapDescriptorFactory.fromBitmap(mifoto))
                            val zoomLevel = 15.0f // Puedes ajustar este valor según sea necesario
                            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
                            mMap.moveCamera(cameraUpdate)
                        }
                }
            }
        }
        return locationCallback
    }

    private lateinit var pantalla: String
    private lateinit var idPlan: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityElegirUbicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        pantalla= intent.getStringExtra("pantalla").toString()
        idPlan= intent.getStringExtra("idPlan").toString()
        idGrupo=intent.getStringExtra("idGrupo").toString()
        Log.e("idGrupo", "revisar seleccionar Ubicacion $idGrupo")
        inicializarBotones()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //poner ubicacion seleccionada en recomendaciones
        inicializarImagen()

        if("recomendacion".equals(intent.getStringExtra("recomendacion").toString()))
        {
            Log.i("entre","entre desde recomendaciones")
            //poner la posicion
            firstTime=false
            longActual= intent.getDoubleExtra("longitud", 0.0)
            latActual= intent.getDoubleExtra("latitud", 0.0)
            var pos=LatLng(latActual,longActual)
            MarkerActual?.remove()
            MarkerActual=mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .icon(imagenPin?.let { it1 -> BitmapDescriptorFactory.fromBitmap(it1) })
                    .title("Pos seleccionada"))
            //.icon(BitmapDescriptorFactory.fromBitmap(mifoto))
            val zoomLevel = 15.0f // Puedes ajustar este valor según sea necesario
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
            mMap.moveCamera(cameraUpdate)
        }

        configurarLocalizacion()

        mMap.setOnMapLongClickListener {

            MarkerActual?.remove()
            //poner la foto
            MarkerActual=mMap.addMarker(MarkerOptions()
                .icon(imagenPin?.let { it1 -> BitmapDescriptorFactory.fromBitmap(it1) })
                .position(it)
                .title("Marker"))
            latActual=it.latitude
            longActual=it.longitude
            var pos=LatLng(latActual,longActual)
            val zoomLevel = 15.0f // Puedes ajustar este valor según sea necesario
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
            mMap.moveCamera(cameraUpdate)
        }
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()

    }
    override fun onResume() {
        super.onResume()

        // Registrar el SensorEventListener para el sensor de luz
        //sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onRestart() {
        super.onRestart()
        startLocationUpdates()
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
    private fun stopLocationUpdates() {
        location.removeLocationUpdates(locationCallBack)

    }

    val db = Firebase.firestore
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference
    private fun inicializarImagen() {
        try {
            val file = File(getExternalFilesDir(null), "plan.json")
            val json_string = file.bufferedReader().use {
                it.readText()
            }

            val planJsonArray = JSONArray(json_string)

            for (i in 0 until planJsonArray.length()) {

                val jsonObject = planJsonArray.getJSONObject(i)

                val fotopinBase64 = jsonObject.getString("fotoPinGrande")
                val fotopinByteArray = Base64.decode(fotopinBase64, Base64.DEFAULT)
                loadImage(ByteArrayInputStream(fotopinByteArray))

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

    private fun loadImage(imageStream:  InputStream?) {
        var srcBitmap = BitmapFactory.decodeStream(imageStream)

        srcBitmap=createCircledImage(srcBitmap)
        binding.pinElegirUbicacion.setImageBitmap(srcBitmap)

        imagenPin = Bitmap.createScaledBitmap(srcBitmap, 160, 160, true)

        //srcBitmap.recycle()
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

    /*
    fun createLightSensorListener() : SensorEventListener{
        val ret : SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if(this@ElegirUbicacionActivity::map.isInitialized){
                    if (event != null && event.sensor.type == Sensor.TYPE_LIGHT) {
                        if(event.values[0] < 1500){
                            // Cambiar a modo oscuro
                            map.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
                        }else{
                            // Cambiar a modo claro
                            map.getOverlayManager().getTilesOverlay().setColorFilter(null);
                        }
                    }
                }
            }
            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            }
        }
        return ret
    }*/

    private fun inicializarBotones() {

        binding.verRecomendacion.setOnClickListener {
            //clearOsmdroidTileCache()
            val intent=Intent(baseContext, RecomendacionesActivity::class.java)
            intent.putExtra("pantalla",pantalla)
            intent.putExtra("idPlan", idPlan)
            intent.putExtra("idGrupo", idGrupo)
            intent.putExtra("longitud", longMia)
            intent.putExtra("latitud", latMia)
            startActivity(intent)
        }

        binding.guardar.setOnClickListener {

            val intent: Intent = if(pantalla == "crear") {
                Intent(baseContext, CrearPlanActivity::class.java)
            } else {
                Intent(baseContext, EditarPlanActivity::class.java)
            }

// Agregar los valores de longitud y latitud como extras al Intent
            intent.putExtra("longitud", longActual)
            intent.putExtra("latitud", latActual)
            intent.putExtra("pantalla", "ubicacion")
            intent.putExtra("idPlan", idPlan)
            intent.putExtra("idGrupo", idGrupo)

// Iniciar la siguiente actividad con el Intent modificado
            startActivity(intent)

        }
        val usuario: UsuarioAmigo = UsuarioAmigo()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    startActivity(Intent(baseContext, VerGruposActivity::class.java))
                    true
                }
                R.id.cuenta_bar -> {
                    val executor = ContextCompat.getMainExecutor(this)
                    val biometricPrompt = BiometricPrompt(this, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                // Aquí puedes realizar alguna acción después de la autenticación exitosa
                                // Por ejemplo, mostrar un mensaje o iniciar una nueva actividad
                                var intent = Intent(baseContext, PerfilConfActivity::class.java)
                                intent.putExtra("user", usuario)
                                startActivity(intent)
                                //startActivity(Intent(baseContext, PerfilConfActivity::class.java))
                                //startActivity(Intent(baseContext, VerGruposActivity::class.java))
                                true
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Autenticación de huella dactilar")
                        .setSubtitle("Toque el sensor de huella dactilar")
                        .setNegativeButtonText("Cancelar")
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                    // Respond to navigation item 2 click
                    false
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

    private fun fabClicks() {
        binding.fabPlanesPasados.setOnClickListener {
            var intent = Intent(baseContext, PlanesPasadosActivity::class.java)
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.fabCrearPlan.setOnClickListener {
            var intent = Intent(baseContext, CrearPlanActivity::class.java)
            intent.putExtra("pantalla", "planes")
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
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
        val fechaActual = LocalDateTime.now()

        val formatoFecha = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Establece la zona horaria a UTC si es necesario
        formatoFecha.timeZone = TimeZone.getTimeZone("UTC")
        formatoHora.timeZone = TimeZone.getTimeZone("UTC")

        val fechaHoraAlarmaInicio =textoAFechaAlarma(
            formatoFecha.format(dateInicio).toString(),
            formatoHora.format(dateInicio).toString()
        )

        val fechaHoraAlarmaFinal =textoAFechaAlarma(
            formatoFecha.format(dateFinal).toString(),
            formatoHora.format(dateFinal).toString()
        )

        return when {
            fechaActual<fechaHoraAlarmaInicio -> "Activo"
            fechaActual>fechaHoraAlarmaFinal -> "Cerrado"
            fechaActual>fechaHoraAlarmaInicio && fechaActual<fechaHoraAlarmaFinal-> "Abierto"
            else ->"Abierto"
        }
    }

    fun textoAFechaAlarma(fechaTexto: String, horaTexto: String): LocalDateTime {
        // Parsear los textos de fecha y hora en LocalDateTime
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")

        // Parsear los textos de fecha y hora en LocalDateTime
        val fechaHora = LocalDateTime.parse("${fechaTexto} ${horaTexto}", formatter)
        Log.i("tiempo","es: $fechaHora")
        // Calcular la diferencia en segundos entre la hora actual y la fechaHora propuesta
        val diferenciaSegundos = LocalDateTime.now().until(fechaHora, java.time.temporal.ChronoUnit.SECONDS)
        Log.i("tiempo","es: diferencias local ${LocalDateTime.now()} con  inicio $diferenciaSegundos")
        // Ajustar la hora actual sumando la diferencia en segundos
        return LocalDateTime.now().plusSeconds(diferenciaSegundos)
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

    private fun configurarLocalizacion() {
        /*if (intent.hasExtra("recomendacion") && intent.getBooleanExtra("recomendacion", true)) {
            map.getTileProvider().clearTileCache()
            // Verificar si se recibió un intent con la bandera "recomendacion" establecida como verdadera
            latActual = intent.getDoubleExtra("latitud", 0.0)
            longActual = intent.getDoubleExtra("longitud", 0.0)
            Log.i(ContentValues.TAG, "Info enviar - Longitud: $longActual, Latitud: $latActual")
            // Usar la ubicación proporcionada en el intent
            // Por ejemplo, podrías mostrar esta ubicación en el mapa
            posActualGEO=GeoPoint(latActual, longActual)
            map.controller.animateTo(posActualGEO)
            map.controller.setZoom(19.0)
            selectedLocationOnMap(posActualGEO)

        } else {*/
            // Si no se recibió la bandera "recomendacion" o está establecida como falsa,
            // continuar con la configuración normal de la ubicación
            location = LocationServices.getFusedLocationProviderClient(this)
            locationRequest = createLocationRequest()
            locationCallBack = createLocationCallback()

            // Gestionar los permisos
            gestionarPermiso()
        //}
    }

    private fun createLocationRequest():LocationRequest
    {
        val request=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 7000)
            .setMinUpdateIntervalMillis(2000)
            .setWaitForAccurateLocation(true)
            .build()

        return request
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

    private fun getBitmapFromDrawable(iconID: Int, sizeInDp: Int): Bitmap {
        val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDp.toFloat(), resources.displayMetrics).toInt()
        val drawable = BitmapFactory.decodeResource(resources, iconID)
        return Bitmap.createScaledBitmap(drawable, sizeInPixels, sizeInPixels, false)
    }
}