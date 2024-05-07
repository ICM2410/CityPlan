package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
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
import androidx.core.view.isVisible
import com.example.primeraentrega.Clases.Plan
import com.example.primeraentrega.databinding.ActivityPlanBinding
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
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.min
import android.hardware.SensorEventListener
import com.example.primeraentrega.Clases.Usuario
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.views.overlay.TilesOverlay

class PlanActivity : AppCompatActivity(), SensorEventListener, OnMapReadyCallback {

    private lateinit var binding : ActivityPlanBinding

    //PARA POSICIONES
    private lateinit var mMap: GoogleMap
    private var latActual:Double= 4.0
    private var longActual:Double= 72.0
    private var MarkerActual: com.google.android.gms.maps.model.Marker? = null
    private var  myLocationMarker: com.google.android.gms.maps.model.Marker? = null
    private var  planLocationMarker: com.google.android.gms.maps.model.Marker? = null
    private val mapaDeParticipantes: HashMap<String, com.google.android.gms.maps.model.Marker?> = HashMap()
    private var latEncuentro:Double= -122.0
    private var longEncuentro:Double= 37.0
    private var pasosAvtivado=true
    private var EstoyEnElPlan=true
    private lateinit var roadManager: RoadManager
    private var firstTime=true

    private val localPermissionName=android.Manifest.permission.ACCESS_FINE_LOCATION;
    lateinit var location: FusedLocationProviderClient


    val db = Firebase.firestore
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference

    //MAPA
    private lateinit var geocoder: Geocoder

    //Sensores
    private lateinit var sensorManager: SensorManager
    //Para contar pasos
    private var stepSensor : Sensor? = null
    private lateinit var stepSensorEventListener: SensorEventListener
    private var running = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    //SENSOR luz
    private lateinit var lightSensor : Sensor
    private lateinit var lightEventListener: SensorEventListener
    //Sensor Temperatura
    private  var temperatureSensor: Sensor? = null
    private lateinit var tempEventListener: SensorEventListener


    val permissionRequest= registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            locationSettings()
        })

    //evaluar el gps . si esta prendido o no no existe
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

    //LOCALIZACION
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback

    fun startLocationUpdates()
    {
        if(ActivityCompat.checkSelfPermission(this, localPermissionName)== PackageManager.PERMISSION_GRANTED)
        {
            location.requestLocationUpdates(locationRequest,locationCallBack, Looper.getMainLooper())

            //PARA PONER LA POSICION INICIAL DEL USUARIO
            location.lastLocation.addOnSuccessListener {
                if (it != null) {
                    latActual=it.latitude
                    longActual=it.longitude
                    //auth.currentUser?.uid?.let { databaseReference.child(it).child("latitud").setValue(latActual)}
                    //auth.currentUser?.uid?.let { databaseReference.child(it).child("latitud").setValue(longActual)}

                }
            }
        }
        else
        {
            //Toast.makeText(getApplicationContext(), "NO HAY PERMISO", Toast.LENGTH_LONG).show();
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
                {//Toast.makeText(getApplicationContext(), "($last.latitude , $last.longitude)", Toast.LENGTH_LONG).show();

                    latActual=last.latitude
                    longActual=last.longitude

                    if(EstoyEnElPlan)
                    {
                        var pos=LatLng(latActual,longActual)
                        MarkerActual?.remove()
                        MarkerActual=mMap.addMarker(MarkerOptions().position(pos).title("YO"))

                        if(firstTime)
                        {
                            firstTime=false
                            zoom()
                        }
                    }
                }
            }
        }

        return locationCallback
    }

    private fun zoom() {
        var pos=LatLng(latActual,longActual)
        val zoomLevel = 15.0f // Puedes ajustar este valor según sea necesario
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
        mMap.moveCamera(cameraUpdate)
    }

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


    fun gestionarPermisoActividad() {
        val permissionName = android.Manifest.permission.ACTIVITY_RECOGNITION

        if (ActivityCompat.checkSelfPermission(this, permissionName) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(permissionName)) {
                // Mostrar un mensaje explicativo si es necesario
                Toast.makeText(getApplicationContext(), "La aplicación requiere permiso de reconocimiento de actividad", Toast.LENGTH_LONG).show()
            }
            permissionRequest.launch(permissionName)
        }
    }

    private lateinit var idPlan : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idPlan=intent.getStringExtra("idPlan").toString()

        Log.e(TAG, "revisar $idPlan")

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //configurarConFireBase()

        configurarBotones();

        //configurarLocalizacion()

        configurarSensores()
    }
    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }
    override fun onResume() {
        super.onResume()
        /*
        running = true
        // Registrar el SensorEventListener para el sensor de pasos
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Toast.makeText(this, "No se detectó sensor de pasos", Toast.LENGTH_SHORT).show()
        } else {
            Log.i("Sensor", "Hay podómetro para pasos")
            stepSensorEventListener = createStepSensorListener()
            sensorManager.registerListener(stepSensorEventListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Sensor temperatura
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (temperatureSensor == null) {
            Toast.makeText(this, "No se detectó sensor de temperatura", Toast.LENGTH_SHORT).show()
        } else {
            Log.i("Sensor", "Hay sensor de temperatura")
            tempEventListener = createTemperatureSensorListener()
            sensorManager.registerListener(tempEventListener, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Registrar el SensorEventListener para el sensor de luz
        sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
*/
        //startLocationUpdates()
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
    private fun stopLocationUpdates() {
        location.removeLocationUpdates(locationCallBack)
    }
    private fun configurarConFireBase() {

        Log.d(ContentValues.TAG, "entreee $idPlan")

        val docRef = db.collection("Planes").document(idPlan)
        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d(ContentValues.TAG, "encontrado - ${documentSnapshot.id} => ${documentSnapshot.data}")
                    // Aquí puedes acceder a los datos del documento utilizando document.data
                    val plan = documentSnapshot.toObject<Plan>()
                    //AQUI SE OBTIENE LA INFORMACION PARA PONER
                    //TITULO DEL PLAN
                    binding.tituloPlan.setText(plan?.titulo)
                    //UBICACION DEL PLAN
                    if (plan != null) {
                        Log.d(ContentValues.TAG, "${plan.latitude} y tambien ${plan.longitude} ")
                        latEncuentro=plan.latitude
                        longEncuentro=plan.longitude
                        ponerUbicacionPlan()
                    }
                    //SI TIENE LO DE NUMERO DE PASOS
                    if (plan != null) {
                        pasosAvtivado=plan.AmigoMasActivo
                        if(!pasosAvtivado){

                            // Hacer invisible el elemento binding.pasoscantText
                            binding.pasoscantText.visibility = View.INVISIBLE
                        }
                    }

                    val pathReferencePin = plan?.let { storageRef.child(it.fotopin) }

                    val ONE_MEGABYTE: Long = 1024 * 1024
                    pathReferencePin?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                        // Los bytes de la imagen se han recuperado exitosamente
                        val imageStream = ByteArrayInputStream(bytes)
                        //loadImage(imageStream)
                    }?.addOnFailureListener {
                        // Manejar cualquier error que ocurra durante la recuperación de la imagen
                    }

                    //ESTO ES DEL USUARIO
                    if(EstoyEnElPlan)
                    {
                        binding.switchPasos.isChecked=true
                        binding.aunsiguesText.setText("Aun sigues en el plan")
                    }
                    else
                    {
                        binding.aunsiguesText.setText("Estas fuera del plan")
                        binding.switchPasos.isChecked=false
                        // Hacer invisible el elemento binding.hazDado
                        // Hacer invisible el elemento binding.pasoscantText
                        binding.pasoscantText.visibility = View.INVISIBLE

                        binding.mostrarRutabutton.isVisible= false
                        binding.milocalizacion.isVisible=false
                        stopLocationUpdates()
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "get failed with ", exception)
            }
    }

    private fun configurarSensores(){
        //Sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //Sensor Luz
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        //lightEventListener = createLightSensorListener()
        //Sensor Pasos
        loadData()
        resetSteps()
    }
    fun createStepSensorListener() : SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (running && event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    // Incrementar el contador de pasos cada vez que se detecta un paso
                    binding.pasoscantText.text = (binding.pasoscantText.text.toString().toInt() + 1).toString()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No necesitas hacer nada aquí para este caso
            }
        }
    }
    /*fun createLightSensorListener() : SensorEventListener{
        val ret : SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if(this@PlanActivity::map.isInitialized){
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
    fun createTemperatureSensorListener() : SensorEventListener {
        val ret : SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    val temperatura = event.values[0]
                    val resource = when {
                        temperatura < 0 -> {
                            R.drawable.nevando
                        }
                        temperatura < 15 -> {
                            R.drawable.muynublado
                        }
                        temperatura < 20 -> {
                            R.drawable.parcialmentenublado
                        }
                        else -> {
                            R.drawable.soleado
                        }
                    }
                    binding.imagenTemperatura.setImageResource(resource)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No necesitas hacer nada aquí para este caso
            }
        }
        return ret
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
    override fun onSensorChanged(event: SensorEvent?) {
        if(running){
            totalSteps = event!!.values[0]
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
            binding.pasoscantText.text = ("$currentSteps")

        }
    }
    fun resetSteps(){
        previousTotalSteps = totalSteps
        binding.pasoscantText.text = 0.toString()
        saveData()
    }
    fun saveData(){
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("Key", previousTotalSteps)
        editor.apply()
    }
    private fun loadData(){
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("KEY", 0f)
        Log.d("PlanActivity","$savedNumber")
        previousTotalSteps = savedNumber
    }

    //BOTONES MENU
    private var isFabOpen=false
    private var rotation=false
    private fun  configurarBotones()
    {
        binding.configuraciones.setOnClickListener{

            val intent=Intent(baseContext,EditarPlanActivity::class.java)
            intent.putExtra("idPlan",idPlan)
            startActivity(intent)
        }

        binding.botonCamara.setOnClickListener{
            startActivity(Intent(baseContext,GaleriaActivity::class.java))
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

        initShowout(binding.confView)
        initShowout(binding.rutaView)
        initShowout(binding.recuerdosView)
        binding.fabOpcionesPlan.setOnClickListener {
            if(!isFabOpen)
            {
                showFabPlan();
            }
            else
            {
                closeFabPlan();
            }
        }
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
    private fun closeFabPlan() {
        rotation=rotateFAB()
        isFabOpen=false
        cerrar(binding.confView)
        cerrar(binding.rutaView)
        cerrar(binding.recuerdosView)
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
    private fun showFabPlan() {
        isFabOpen=true

        mostrarPlan(binding.confView)
        mostrarPlan(binding.rutaView)
        mostrarPlan(binding.recuerdosView)
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
    private fun mostrarPlan(view: View) {
        view.apply {
            visibility= View.VISIBLE
            alpha=0f
            translationY=-height.toFloat()
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


    private var switchRuta=false
    private fun configurarLocalizacion() {
        Log.i("perrito","ji")
        location= LocationServices.getFusedLocationProviderClient(this);
        locationRequest=createLocationRequest()
        locationCallBack=createLocationCallback()

        //primero gestionar los permisos
        gestionarPermiso()
        gestionarPermisoActividad()
        ponerUbicacionPlan()

        binding.mostrarRutabutton.setOnClickListener{
            //muestra la ruta con oms bonus
            if(!switchRuta) {
                switchRuta=true
                binding.mostrarRutaTxt.setText("Quitar ruta")
                var posEncuentroGEO = GeoPoint(latEncuentro, longEncuentro )
                var posActualGEO = GeoPoint(latActual, longActual )
                mostrarRuta(posActualGEO, posEncuentroGEO)
            }
            else
            {
                switchRuta=false
                //quita la ruta si esta existe
                if( polyline!=null) polyline!!.remove()
                binding.mostrarRutaTxt.setText("Mostrar ruta")
            }
        }

        binding.puntoEncuentro.setOnClickListener{
            //lo centra a la ubicacion del punto de encuentro
            var pos=LatLng(latEncuentro,longEncuentro)
            val zoomLevel = 15.0f // Puedes ajustar este valor según sea necesario
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
            mMap.moveCamera(cameraUpdate)
        }

        binding.milocalizacion.setOnClickListener{
            //lo centra a la ubicacion de mi ubicacion actual
            //centrarse
            zoom()
        }

        //ver si esta prendido o apagado
        binding.switchPasos.setOnClickListener {
            if(binding.switchPasos.isChecked)
            {
                if(pasosAvtivado)
                {
                    // Hacer invisible el elemento binding.pasoscantText
                    binding.pasoscantText.visibility = View.VISIBLE
                }
                binding.mostrarRutaTxt.setText("Mostrar ruta")
                binding.milocalizacion.isVisible=true
                binding.mostrarRutabutton.isVisible= true
                binding.aunsiguesText.setText("Aun sigues en el plan")
                EstoyEnElPlan=true
                startLocationUpdates()
                var pos=LatLng(latActual,longActual)
                MarkerActual?.remove()
                MarkerActual=mMap.addMarker(MarkerOptions().position(pos).title("YO"))
                zoom()
            }
            else
            {
                binding.milocalizacion.isVisible=false
                // Hacer invisible el elemento binding.pasoscantText
                binding.pasoscantText.visibility = View.INVISIBLE

                binding.mostrarRutabutton.isVisible= false
                binding.aunsiguesText.setText("Estas fuera del plan")
                stopLocationUpdates()
                MarkerActual?.remove()
                if( polyline!=null) polyline!!.remove()
            }
        }
    }

    private fun ponerUbicacionPlan() {
        var pos=LatLng(latActual,longActual)
        planLocationMarker?.remove()
        planLocationMarker=mMap.addMarker(MarkerOptions().position(pos).title("PLAN"))
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
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        configurarConFireBase()
        configurarLocalizacion()
        var pos=LatLng(latActual,longActual)
        val zoomLevel = 15.0f // Puedes ajustar este valor según sea necesario
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
        mMap.moveCamera(cameraUpdate)
    }

    private val COLOR_BLACK_ARGB = -0x1000000
    private val POLYLINE_STROKE_WIDTH_PX = 12
    private var polyline: com.google.android.gms.maps.model.Polyline?=null
    private fun mostrarRuta(start : GeoPoint, finish : GeoPoint){
        Thread(Runnable {
            var routePoints = ArrayList<GeoPoint>()
            routePoints.add(start)
            routePoints.add(finish)
            val road = roadManager.getRoad(routePoints)
            Log.i("MapsApp", "Route length: " + road.mLength + " klm")
            Log.i("MapsApp", "Duration: " + road.mDuration / 60 + " min")
            Log.i("MapsApp", "Points: " + road.mRouteHigh + " min")
            val puntos = road.mRouteHigh

            // Crear una lista vacía para almacenar los LatLng resultantes
            val listaLatLng = mutableListOf<LatLng>()

            // Recorrer la lista de GeoPoints y convertir cada uno a LatLng
            for (punto in puntos) {
                val latLng = LatLng(punto.latitude, punto.longitude)
                listaLatLng.add(latLng)
            }

            // Actualizar el UI en el hilo principal
            runOnUiThread {
                // Usar la listaLatLng en la creación del Polyline
                if( polyline!=null) polyline!!.remove()

                polyline= mMap.addPolyline(
                    PolylineOptions()
                    .clickable(true)
                    .addAll(listaLatLng))
                polyline!!.setWidth(POLYLINE_STROKE_WIDTH_PX.toFloat())
                polyline!!.setColor(COLOR_BLACK_ARGB)
                polyline!!.setJointType(JointType.ROUND)
            }
        }).start()

    }
}