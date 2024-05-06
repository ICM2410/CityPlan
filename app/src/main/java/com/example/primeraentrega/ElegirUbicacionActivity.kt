package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.databinding.ActivityElegirUbicacionBinding
import com.example.primeraentrega.Clases.Usuario
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import kotlin.math.min

class ElegirUbicacionActivity : AppCompatActivity() {

    private lateinit var binding : ActivityElegirUbicacionBinding

    //PARA POSICIONES
    private var posActualGEO = GeoPoint(4.0, 72.0)
    private var latActual:Double= 4.0
    private var longActual:Double= 72.0
    private var isFabOpen=false
    private var rotation=false

    private val localPermissionName=android.Manifest.permission.ACCESS_FINE_LOCATION;
    lateinit var location: FusedLocationProviderClient

    //MAPA
    lateinit var map : MapView
    private lateinit var geocoder: Geocoder

    //LOCALIZACION
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback

    private lateinit var bitmap:Bitmap

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

    fun gestionarPermiso()
    {
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
                        posActualGEO=GeoPoint(last.latitude, last.longitude)

                        if(firstTime)
                        {
                            firstTime=false
                            map.controller.animateTo(posActualGEO)
                            map.controller.setZoom(19.0)
                            selectedLocationOnMap(posActualGEO)
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

        pantalla= intent.getStringExtra("pantalla").toString()
        idPlan= intent.getStringExtra("idPlan").toString()

        inicializarBotones()

        inicializarImagen()

        configurarMapa()

        configurarLocalizacion()


        //Sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //Sensor Luz
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        lightEventListener = createLightSensorListener()

    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()

    }

    override fun onResume() {
        super.onResume()

        // Registrar el SensorEventListener para el sensor de luz
        sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)

        map.onResume()
        map.controller.setZoom(19.0)
        map.controller.animateTo(posActualGEO)
        startLocationUpdates()
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

       val docRef = db.collection("Planes").document(idPlan)
        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d(ContentValues.TAG, "encontrado elegir - ${documentSnapshot.id} => ${documentSnapshot.data}")
                    // Aquí puedes acceder a los datos del documento utilizando document.data
                    val plan = documentSnapshot.toObject<Plan>()

                    //obtener imagenes
                    val pathReferencePin = plan?.let { storageRef.child(it.fotopin) }

                    val ONE_MEGABYTE: Long = 1024 * 1024
                    pathReferencePin?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                        // Los bytes de la imagen se han recuperado exitosamente

                        if (bytes != null) {
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                            // Crear un bitmap cuadrado con el tamaño máximo entre el ancho y el alto de la imagen
                            val squareBitmap = Bitmap.createBitmap(
                                min(bitmap.width, bitmap.height),
                                min(bitmap.width, bitmap.height),
                                Bitmap.Config.ARGB_8888
                            )

                            // Crear un lienzo para dibujar en el bitmap cuadrado
                            val canvas = Canvas(squareBitmap)

                            // Dibujar la imagen original en el centro del bitmap cuadrado
                            val left = (squareBitmap.width - bitmap.width) / 2f
                            val top = (squareBitmap.height - bitmap.height) / 2f
                            canvas.drawBitmap(bitmap, left, top, null)

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

                            binding.pinElegirUbicacion.setImageBitmap(circleBitmap)

                        } else {
                            // Manejar el caso en el que no se haya pasado ningún byteArray en el intent
                        }

                    }?.addOnFailureListener {
                        // Manejar cualquier error que ocurra durante la recuperación de la imagen
                    }


                } else {
                    Log.d(ContentValues.TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "get failed with ", exception)
            }
    }

    //MAPA
    private fun configurarMapa() {
        Configuration.getInstance().load(this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        geocoder = Geocoder(baseContext)
        map = binding.osmMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.overlays.add(createOverlayEvents())
    }

    fun createOverlayEvents() : MapEventsOverlay {
        val overlayEvents = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if(p!=null) {
                    latActual=p.latitude
                    longActual=p.longitude

                    Log.i(ContentValues.TAG, "HELPER - Longitud: $longActual, Latitud: $latActual")

                    selectedLocationOnMap(p)
                }
                return true
            }
        })
        return overlayEvents
    }

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
    }

    private fun inicializarBotones() {

        binding.verRecomendacion.setOnClickListener {
            //clearOsmdroidTileCache()
            val intent=Intent(baseContext, RecomendacionesActivity::class.java)
            intent.putExtra("pantalla",pantalla)
            intent.putExtra("idPlan", idPlan)
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

// Iniciar la siguiente actividad con el Intent modificado
            startActivity(intent)

        }
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

        fabClicks()
    }

    private fun fabClicks() {
        binding.fabPlanesPasados.setOnClickListener {
            startActivity(Intent(baseContext, PlanesPasadosActivity::class.java))
        }

        binding.fabCrearPlan.setOnClickListener {
            startActivity(Intent(baseContext, CrearPlanActivity::class.java))
        }

        binding.fabMisPlanes.setOnClickListener {
            startActivity(Intent(baseContext, PlanesActivity::class.java))
        }

        binding.fabPlanActivo.setOnClickListener {
            startActivity(Intent(baseContext, PlanActivity::class.java))
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


    private var  selectedLocationMarker: Marker? = null
    fun selectedLocationOnMap(p: GeoPoint){
        if(selectedLocationMarker!=null)
            map.overlays.remove(selectedLocationMarker)
        val address = findAddress(LatLng(p.latitude, p.longitude))
        val snippet : String
        if(address!=null) {
            snippet = address
        }else{
            snippet = ""
        }
        addMarker(p, snippet, 0)
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

    fun addMarker(p: GeoPoint, snippet : String, tipo : Int){
        //MY LOCATION
        selectedLocationMarker = createMarker(p, "Ubicacion seleccionada", snippet, R.drawable.pinyo)

        if (selectedLocationMarker != null) {
            map.getOverlays().add(selectedLocationMarker)
        }
    }

    val sizeInDp = 70
    //val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDp.toFloat(), resources.displayMetrics).toInt()
    //@SuppressLint("SuspiciousIndentation")
    fun createMarker(p: GeoPoint, title: String, desc: String, iconID:Int): Marker? {//bitmap: Bitmap) : Marker? {
        var marker : Marker? = null;
        if(map!=null) {
            marker = Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);

            if (iconID != 0) {
                val bitmap = getBitmapFromDrawable(iconID, 60)
                val drawable = BitmapDrawable(resources, bitmap)
                marker.icon = drawable
            }
            val MAX_ICON_SIZE = 130
            //val originalBitmap = bitmap
            // Establecer el BitmapDrawable como el icono del Marker
            //marker.setIcon(drawable)

            marker.setPosition(p)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.setPosition(p);
            marker.setAnchor(
                Marker.
                ANCHOR_CENTER, Marker.
                ANCHOR_BOTTOM);
        }
        return marker
    }

    private fun getBitmapFromDrawable(iconID: Int, sizeInDp: Int): Bitmap {
        val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDp.toFloat(), resources.displayMetrics).toInt()
        val drawable = BitmapFactory.decodeResource(resources, iconID)
        return Bitmap.createScaledBitmap(drawable, sizeInPixels, sizeInPixels, false)
    }
}