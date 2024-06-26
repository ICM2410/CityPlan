package com.example.primeraentrega

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.ContentValues
import android.content.ContentValues.TAG
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
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
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
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import kotlin.math.min
import android.hardware.SensorEventListener
import com.example.primeraentrega.Clases.Estadistica
import com.example.primeraentrega.Clases.PosAmigo
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

class PlanActivity : AppCompatActivity(), SensorEventListener, OnMapReadyCallback {

    private lateinit var binding : ActivityPlanBinding

    //PARA POSICIONES
    private var plan: Plan? = null

    private lateinit var mMap: GoogleMap
    private var latActual:Double= 4.0
    private var longActual:Double= 72.0
    private var MarkerActual: com.google.android.gms.maps.model.Marker? = null
    private var  planLocationMarker: com.google.android.gms.maps.model.Marker? = null
    private val mapaDeParticipantes: MutableMap<String?, com.google.android.gms.maps.model.Marker?> = mutableMapOf()
    private val mapaDeFotoParticipantes: MutableMap<String?, Bitmap> = mutableMapOf()
    private var latEncuentro:Double= -122.0
    private var longEncuentro:Double= 37.0
    private var pasosAvtivado=true
    private var EstoyEnElPlan=true
    private lateinit var roadManager: RoadManager
    private var firstTime=true
    val miImagenResource = R.drawable.pinyo

    private lateinit var mifoto: Bitmap
    private lateinit var fotoPlan:Bitmap

    private val localPermissionName=android.Manifest.permission.ACCESS_FINE_LOCATION;
    lateinit var location: FusedLocationProviderClient
    private lateinit var idGrupo : String

    private lateinit var auth:FirebaseAuth
    private lateinit var databaseReferencePlanes: DatabaseReference
    private lateinit var databaseReferenceGrupos: DatabaseReference
    private lateinit var databaseReferenceUsers: DatabaseReference
    private lateinit var database : FirebaseDatabase

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


    //Giroscopio
    private var orientationSensor: Sensor? = null

    //SENSOR luz
    private lateinit var lightSensor : Sensor

    private var nombreUsuario="NOMBRE"

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
            firstTime=true
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

                    //ACTUALIZAR MI UBICACION EN EL RTDB de plan

                    if(EstoyEnElPlan)
                    {
                        var pos=LatLng(latActual,longActual)
                        MarkerActual?.remove()
                        MarkerActual=mMap.addMarker(MarkerOptions()
                            .position(pos)
                            .icon(BitmapDescriptorFactory.fromBitmap(mifoto))
                            .title("YO"))

                        auth.currentUser?.uid?.let { userId ->
                            databaseReferenceUsers.child(userId).apply {
                                child("latitud").setValue(latActual)
                                child("longitud").setValue(longActual)
                            }
                        }

                        val myPos= PosAmigo(
                            latActual,
                            longActual,
                            auth.currentUser?.uid,
                            "usuarios/${auth.currentUser?.uid}.jpg",
                            nombreUsuario
                        )

                        //aqui actualizo mi posicion en el plan
                        auth.currentUser?.let {
                            databaseReferencePlanes.child(idPlan).child("integrantes").child(
                                it.uid).setValue(myPos).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.i("mi pos","posicion actualizada $latActual,$longActual")
                                } else {
                                    Log.i("mi pos","posicion ERROR $latActual,$longActual")

                                }
                            }
                        }

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
            locationSettings()
            startLocationUpdates()
        }
    }


    private lateinit var idPlan : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idPlan=intent.getStringExtra("idPlan").toString()
        idGrupo=intent.getStringExtra("idGrupo").toString()
        Log.e(TAG, "revisar $idPlan")
        Log.e("idGrupo", "revisar $idGrupo")

        auth=FirebaseAuth.getInstance()
        databaseReferencePlanes= FirebaseDatabase.getInstance().getReference("Planes")
        databaseReferenceGrupos= FirebaseDatabase.getInstance().getReference("Groups")
        databaseReferenceUsers= FirebaseDatabase.getInstance().getReference("Usuario")
        database = FirebaseDatabase.getInstance()
        mifoto= BitmapFactory.decodeResource(resources, miImagenResource)
        fotoPlan= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, miImagenResource), 160, 160, true)

        roadManager = OSRMRoadManager(this, "ANDROID")

        stepSensorEventListener = createStepSensorEventListener()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        configurarBotones();

        configurarSensores()
    }
    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }
    override fun onRestart() {
        super.onRestart()
        firstTime=true
        configurarConFireBaseFotos()
        configurarLocalizacion()
        startLocationUpdates()
    }
    override fun onResume() {
        super.onResume()

        running = true
        sensorManager.registerListener(
            stepSensorEventListener,
            stepSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        // Registrar el SensorEventListener para el sensor de pasos
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        //stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Toast.makeText(this, "No se detectó sensor de pasos", Toast.LENGTH_SHORT).show()
        } else {
            Log.i("Sensor", "Hay podómetro para pasos")
            //stepSensorEventListener = createStepSensorListener()
            sensorManager.registerListener(stepSensorEventListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }

        if (orientationSensor == null) {
            Toast.makeText(this, "No se detectó sensor de orientacion", Toast.LENGTH_SHORT).show()
        } else {
            Log.i("Sensor", "Hay orientacion")
            //stepSensorEventListener = createStepSensorListener()
            sensorManager.registerListener(orientationEventListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    private val orientationEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationValues)

                // El ángulo de orientación se encuentra en orientationValues[0] en radianes
                // Convierte el ángulo de radianes a grados
                val azimuthDegrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()

                // Actualizar la orientación del mapa en Google Maps con el ángulo de azimuth en grados
                updateMapOrientation(azimuthDegrees)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // No necesitas implementar esto para este ejemplo
        }
    }

    private fun updateMapOrientation(azimuth: Float) {
        if (::mMap.isInitialized) {
            // Girar el mapa en Google Maps utilizando el ángulo de azimuth
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                    .target(mMap.cameraPosition.target) // Mantener el mismo centro del mapa
                    .zoom(mMap.cameraPosition.zoom) // Mantener el mismo nivel de zoom
                    .bearing(azimuth) // Girar el mapa según el ángulo de azimuth
                    .tilt(0f) // Mantener el mismo ángulo de inclinación
                    .build()
            ))
        }
    }



    override fun onPause() {
        super.onPause()
        running = false
        sensorManager.unregisterListener(stepSensorEventListener)
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        location.removeLocationUpdates(locationCallBack)
    }
    private fun configurarConFireBase() {
        anadirPinUsuario {listo->
            val planRef = database.getReference("Planes").child(idPlan)

            planRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Aquí puedes obtener los datos del usuario desde dataSnapshot
                    plan = dataSnapshot.getValue(Plan::class.java)
                    if (plan != null) {
                        // Haz lo que necesites con los datos del usuario
                        binding.tituloPlan.setText(plan?.titulo)
                        //UBICACION DEL PLAN

                        //SI TIENE LO DE NUMERO DE PASOS
                        if (plan != null) {
                            pasosAvtivado= plan!!.AmigoMasActivo
                            if(!pasosAvtivado){

                                // Hacer invisible el elemento binding.pasoscantText
                                binding.pasoscantText.visibility = View.INVISIBLE
                            }
                        }

                        val localfile = File. createTempFile( "tempImage", "jpg")

                        val storageRef = FirebaseStorage.getInstance().reference.child(plan!!.fotopin)


                        storageRef.getFile(localfile).addOnSuccessListener {
                            Log.i("fotoPin","ruta ${plan!!.fotopin}")
                            var src = BitmapFactory.decodeFile(localfile.absolutePath)
                            src=createCircledImage(src)
                            fotoPlan = Bitmap.createScaledBitmap(src, 160, 160, true)
                            if (plan != null) {
                                Log.d(ContentValues.TAG, "${plan!!.latitude} y tambien ${plan!!.longitude} ")
                                latEncuentro= plan!!.latitude
                                longEncuentro= plan!!.longitude
                                ponerUbicacionPlan()
                            }
                            //clickLista()
                        }.addOnFailureListener{
                            Log.i("revisar", "no se pudo poner la foto del pin")
                            if (plan != null) {
                                Log.d(ContentValues.TAG, "${plan!!.latitude} y tambien ${plan!!.longitude} ")
                                latEncuentro= plan!!.latitude
                                longEncuentro= plan!!.longitude
                                ponerUbicacionPlan()
                            }
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

                        if (plan != null) {
                            configurarmarkers(plan!!.integrantes)
                        }

                    } else {
                        println("No se encontraron datos para el plan con UID: $idPlan")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Maneja el error en caso de que ocurra
                    println("Error al obtener los datos del usuario: ${databaseError.message}")
                }
            })
        }

    }

    private fun configurarConFireBaseFotos() {

        anadirPinUsuario { listo->
            val planRef = database.getReference("Planes").child(idPlan)
            planRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Aquí puedes obtener los datos del usuario desde dataSnapshot
                    val plan = dataSnapshot.getValue(Plan::class.java)
                    if (plan != null) {
                        // Haz lo que necesites con los datos del usuario
                        binding.tituloPlan.setText(plan?.titulo)
                        //UBICACION DEL PLAN

                        //SI TIENE LO DE NUMERO DE PASOS
                        if (plan != null) {
                            pasosAvtivado=plan.AmigoMasActivo
                            if(!pasosAvtivado){

                                // Hacer invisible el elemento binding.pasoscantText
                                binding.pasoscantText.visibility = View.INVISIBLE
                            }
                        }

                        val localfile = File. createTempFile( "tempImage", "jpg")

                        val storageRef = FirebaseStorage.getInstance().reference.child(plan.fotopin)

                        storageRef.getFile(localfile).addOnSuccessListener {
                            Log.i("fotoPin","ruta ${plan.fotopin}")
                            var src = BitmapFactory.decodeFile(localfile.absolutePath)
                            src=createCircledImage(src)
                            fotoPlan = Bitmap.createScaledBitmap(src, 160, 160, true)
                            if (plan != null) {
                                Log.d(ContentValues.TAG, "${plan.latitude} y tambien ${plan.longitude} ")
                                latEncuentro=plan.latitude
                                longEncuentro=plan.longitude
                                ponerUbicacionPlan()
                            }
                            //clickLista()
                        }.addOnFailureListener{
                            Log.i("revisar", "no se pudo poner la foto del pin")
                            if (plan != null) {
                                Log.d(ContentValues.TAG, "${plan.latitude} y tambien ${plan.longitude} ")
                                latEncuentro=plan.latitude
                                longEncuentro=plan.longitude
                                ponerUbicacionPlan()
                            }
                        }
                    } else {
                        println("No se encontraron datos para el plan con UID: $idPlan")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Maneja el error en caso de que ocurra
                    println("Error al obtener los datos del usuario: ${databaseError.message}")
                }
            })
        }

    }

    private fun createCircledImage(srcBitmap: Bitmap?): Bitmap {
        val squareBitmapWidth = min(srcBitmap!!.width-1, srcBitmap.height-1)

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
        val left = ((squareBitmapWidth - srcBitmap.width-1) / 2).toFloat()
        val top = ((squareBitmapWidth - srcBitmap.height-1) / 2).toFloat()
        canvas.drawBitmap(srcBitmap, left, top, paint)

        return srcBitmap;
    }

    private fun anadirPinUsuario(callback: (String) -> Unit) {
        //consultar bien del usuario
        auth.currentUser?.let { database.getReference("Usuario").child(it.uid) }
            ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Crea un objeto PosAmigo con la información del usuario
                    val usuario = dataSnapshot.getValue(UsuarioAmigo::class.java)

                    if (usuario != null) {
                        nombreUsuario = usuario.username

                        //anadir la foto del usuario
                        val localfile = File.createTempFile("tempImage", "jpg")

                        val storageRef =
                            FirebaseStorage.getInstance().reference.child("usuarios/${usuario.uid}.jpg")

                        storageRef.getFile(localfile).addOnSuccessListener {
                            Log.i("fotoPin", "ruta ${usuario.uid}.fotopin")
                            var src = BitmapFactory.decodeFile(localfile.absolutePath)
                            src = createCircledImage(src)
                            mifoto = Bitmap.createScaledBitmap(src, 160, 160, true)
                            callback("listo")
                        }.addOnFailureListener {
                            Log.i("revisar", "no se pudo poner la foto del pin")
                            callback("listo")
                        }
                    }


                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Maneja el error en caso de que ocurra
                    println("Error al obtener los datos del usuario: ${databaseError.message}")
                }
            })
    }

    private fun configurarmarkers(integrantes: Map<String,PosAmigo>) {
        //se crea un marker para cada uno de los integrantes
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

    private fun createStepSensorEventListener(): SensorEventListener {
       // var flag=0

        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

                        obtenerEstadisticaplan {
                            Log.i("listo", "$it")
                            Log.e("stepcount","revisar $stepCount")
                            countSteps(event)
                        }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No necesitas implementar esto necesariamente, a menos que quieras manejar cambios en la precisión del sensor.
            }
        }
    }

    private fun obtenerEstadisticaplan(callback: (String) -> Unit) {
        auth.currentUser?.let {
            val estRef=FirebaseDatabase.getInstance().getReference("Estadisticas").child(idPlan).child(it.uid)
            estRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        // Iterar sobre los resultados de la consulta
                        Log.i("datasnapshot pasos", "$dataSnapshot")
                        val est= dataSnapshot.getValue(Estadistica::class.java)!!
                        stepCount=est.pasos
                        binding.pasoscantText.text = "$stepCount"
                        callback("listo")
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Manejar el error en caso de que la consulta sea cancelada
                        Log.e(TAG, "Error al realizar la consulta de estadisticas: ${databaseError.message}")
                    }
                })

        }

    }


    private var lastAcceleration = FloatArray(3)
    private var accelerationThreshold = 5.5f // Umbral de aceleración mínima para considerar un paso
    private var stepCount = 0

    private fun countSteps(event: SensorEvent) {
        val currentAcceleration = event.values.clone()

        if (isStep(currentAcceleration)) {
            stepCount++
            updateStepCount(stepCount)
        }

        lastAcceleration = currentAcceleration.clone()
    }

    private fun isStep(currentAcceleration: FloatArray): Boolean {
        // Calcula la diferencia entre la aceleración actual y la aceleración anterior
        val deltaAcceleration = sqrt(
            (currentAcceleration[0] - lastAcceleration[0]).pow(2) +
                    (currentAcceleration[1] - lastAcceleration[1]).pow(2) +
                    (currentAcceleration[2] - lastAcceleration[2]).pow(2)
        )

        // Devuelve true si la diferencia supera el umbral de aceleración
        return deltaAcceleration > accelerationThreshold
    }

    private fun updateStepCount(stepCount: Int) {
        // Actualiza la vista o realiza cualquier otra acción necesaria con el nuevo recuento de pasos
        binding.pasoscantText.text = "$stepCount"
        auth.currentUser?.let {
            val estRef=FirebaseDatabase.getInstance().getReference("Estadisticas").child(idPlan).child(it.uid).child("pasos")
            estRef.setValue(stepCount).addOnCompleteListener {task ->
                if (task.isSuccessful) {
                    Log.i("pasos","guardados $stepCount")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
    override fun onSensorChanged(event: SensorEvent?) {
        /*if(running){
            totalSteps = event!!.values[0]
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
            binding.pasoscantText.text = ("$currentSteps")

        }*/
    }
    fun resetSteps(){
        previousTotalSteps = totalSteps
        //binding.pasoscantText.text = 0.toString()
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
    private fun  configurarBotones(){
        binding.configuraciones.setOnClickListener{

            val intent=Intent(baseContext,EditarPlanActivity::class.java)
            intent.putExtra("idPlan",idPlan)
            intent.putExtra("pantalla","plan")
            intent.putExtra("idGrupo", idGrupo)
            startActivity(intent)
        }

        binding.botonCamara.setOnClickListener {
            val intent = Intent(baseContext, GaleriaActivity::class.java)
            intent.putExtra("idPlan", idPlan)
            intent.putExtra("idGrupo", idGrupo)
            intent.putExtra("nombrePlan", plan?.titulo) // Agregar el nombre del plan como extra
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
                metermeAlPlan()
                startLocationUpdates()
                var pos=LatLng(latActual,longActual)
                MarkerActual?.remove()
                MarkerActual=mMap.addMarker(MarkerOptions()
                    .position(pos)
                    .icon(BitmapDescriptorFactory.fromBitmap(mifoto))
                    .title("YO"))
                zoom()
            }
            else
            {
                binding.milocalizacion.isVisible=false
                // Hacer invisible el elemento binding.pasoscantText
                binding.pasoscantText.visibility = View.INVISIBLE

                binding.mostrarRutabutton.isVisible= false
                binding.aunsiguesText.setText("Estas fuera del plan")
                salirDelPlan()
                stopLocationUpdates()
                mMap.clear()
                MarkerActual?.remove()
                ponerUbicacionPlan()
                if( polyline!=null) polyline!!.remove()
            }
        }
    }

    private fun salirDelPlan() {
        //aqui me quito de la lista de integrantes del plan
        //me quito del plan como tal
        auth.currentUser?.let {
            val usuarioRef = databaseReferencePlanes.child(idPlan).child("integrantes").child(it.uid)
            usuarioRef.removeValue()
                .addOnSuccessListener {
                    // El usuario fue eliminado exitosamente
                    Log.d(TAG, "Usuario eliminado correctamente de 'integrantes'")
                }
                .addOnFailureListener { e ->
                    // Ocurrió un error al intentar eliminar el usuario
                    Log.e(TAG, "Error al eliminar usuario de 'integrantes': ${e.message}", e)
                }
        }
        //me quito del plan del grupo
        val grupoRef = databaseReferenceGrupos.child(idGrupo)
        val planesRef = grupoRef.child("planes")
        val query = planesRef.orderByChild("id").equalTo(idPlan)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Iterar sobre los resultados de la consulta
                for (planSnapshot in dataSnapshot.children) {

                    Log.i("plan sanpshot","$planSnapshot")
                    // Eliminar el plan encontrado utilizando removeValue()
                    auth.currentUser?.uid?.let {uid ->
                        planSnapshot.key?.let { idPlan ->
                            planesRef.child(idPlan).child("integrantes").child(uid).removeValue()
                                .addOnSuccessListener {
                                    // El plan fue eliminado exitosamente
                                    Log.d(TAG, "Integrante eliminado correctamente del plan del grupo")
                                }
                                .addOnFailureListener { e ->
                                    // Ocurrió un error al intentar eliminar el plan
                                    Log.e(TAG, "Error al eliminar integrante del plan del grupo: ${e.message}", e)
                                }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar el error en caso de que la consulta sea cancelada
                Log.e(TAG, "Error al realizar la consulta para eliminar el plan del grupo: ${databaseError.message}")
            }
        })
    }
    private fun metermeAlPlan() {
        //me anado al plan
        val myPos= PosAmigo(
            latActual,
            longActual,
            auth.currentUser?.uid,
            "usuarios/${auth.currentUser?.uid}.jpg",
            nombreUsuario
        )

        auth.currentUser?.let {
            databaseReferencePlanes.child(idPlan).child("integrantes").child(it.uid).setValue(myPos)
                .addOnSuccessListener {
                    // El usuario fue eliminado exitosamente
                    Log.d(TAG, "Usuario anadido correctamnte en 'integrantes'")
                }
                .addOnFailureListener { e ->
                    // Ocurrió un error al intentar eliminar el usuario
                    Log.e(TAG, "Error alanadir usuario en 'integrantes': ${e.message}", e)
                }
        }

        //me anado al plan del grupo

        val grupoRef = FirebaseDatabase.getInstance().getReference("Groups").child(idGrupo)
        val planesRef = grupoRef.child("planes")
        val query = planesRef.orderByChild("id").equalTo(idPlan)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Iterar sobre los resultados de la consulta
                for (planSnapshot in dataSnapshot.children) {
                    // Eliminar el plan encontrado utilizando removeValue()
                    auth.currentUser?.uid?.let { planSnapshot.ref.child("integrantes").child(it).setValue(myPos) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar el error en caso de que la consulta sea cancelada
                Log.e(TAG, "Error al realizar la consulta para eliminar el plan del grupo: ${databaseError.message}")
            }
        })
    }

    private fun ponerUbicacionPlan() {
        var pos=LatLng(latEncuentro,longEncuentro)
        planLocationMarker?.remove()
        planLocationMarker = mMap.addMarker(MarkerOptions()
            .position(pos)
            .icon(BitmapDescriptorFactory.fromBitmap(fotoPlan))
            .title("PLAN"))

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
        obtenerDatosInicialesAmigos {
            subscribirseACambiosDeAmigos()
        }
        subscribirseACambiosDePlan()
        configurarLocalizacion()
        var pos=LatLng(latActual,longActual)
        val zoomLevel = 15.0f // Puedes ajustar este valor según sea necesario
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
        mMap.moveCamera(cameraUpdate)
    }

    private fun subscribirseACambiosDeAmigos() {
        val databaseReference = databaseReferencePlanes.child(idPlan).child("integrantes")
        mapaDeParticipantes.forEach { (key, value) ->
            Log.i("Mapa de participantes gggg44", "Clave: $key, Valor: $value")
        }
        //se va a escuchar el plan y se vera que se cambia
        //se evalua si se cambia algo de info del plan o tambien info de un usuario
        databaseReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                if(dataSnapshot.key!= auth.currentUser?.uid)
                {
                    if(dataSnapshot.key=="integrantes")
                    {
                        for (childSnapshot in dataSnapshot.children) {
                            val posAmigoActualizado = childSnapshot.getValue(PosAmigo::class.java)
                            if (posAmigoActualizado != null && posAmigoActualizado.uid != auth.currentUser?.uid) {
                                // Procesar solo si el UID no es igual a "t6rbpb1u2CS2rCJShRF2LJ4PO0l1"
                                // Aquí añades tu lógica para añadir un nuevo marcador al mapa
                                anadirAmigo(childSnapshot)
                            }
                        }
                    }
                    else if(dataSnapshot.key!= auth.currentUser?.uid)
                    {
                        anadirAmigo(dataSnapshot)
                    }
                }
            }
            override fun onChildChanged(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                if(dataSnapshot.key=="integrantes")
                {
                    for (childSnapshot in dataSnapshot.children) {
                        val posAmigoActualizado = childSnapshot.getValue(PosAmigo::class.java)
                        if (posAmigoActualizado != null && posAmigoActualizado.uid != auth.currentUser?.uid) {
                            // Procesar solo si el UID no es igual a "t6rbpb1u2CS2rCJShRF2LJ4PO0l1"
                            // Aquí añades tu lógica para añadir un nuevo marcador al mapa
                            actualizarAmigo(childSnapshot)
                        }
                    }
                }
                else if(dataSnapshot.key!= auth.currentUser?.uid)
                {
                    actualizarAmigo(dataSnapshot)
                }
            }
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.key=="integrantes")
                {
                    for (childSnapshot in dataSnapshot.children) {
                        val posAmigoActualizado = childSnapshot.getValue(PosAmigo::class.java)
                        if (posAmigoActualizado != null && posAmigoActualizado.uid != auth.currentUser?.uid) {
                            // Procesar solo si el UID no es igual a "t6rbpb1u2CS2rCJShRF2LJ4PO0l1"
                            // Aquí añades tu lógica para añadir un nuevo marcador al mapa
                            eliminarAmigo(childSnapshot)
                        }
                    }
                }
                else if(dataSnapshot.key!= auth.currentUser?.uid)
                {
                    eliminarAmigo(dataSnapshot)
                }
            }
            override fun onChildMoved(dataSnapshot: DataSnapshot, prevChildKey: String?) {
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    private fun eliminarAmigo(dataSnapshot: DataSnapshot?) {
        val posAmigoActualizado = dataSnapshot?.getValue(PosAmigo::class.java)

        if(posAmigoActualizado!=null)
        {
            Log.i("posAmigoActualizado REMOVED"," posAmigoActualizado added: ${posAmigoActualizado.latitud}, ${posAmigoActualizado.longitud}")
            //actualizar pin del usuario en especifico
            if (dataSnapshot != null) {
                mapaDeParticipantes[dataSnapshot.key]?.remove()
            }
        }
    }

    private fun anadirAmigo(dataSnapshot: DataSnapshot?) {
        val posAmigoActualizado = dataSnapshot?.getValue(PosAmigo::class.java)

        if(posAmigoActualizado!=null)
        {
            Log.i("posAmigoActualizado ADDED"," posAmigoActualizado added: ${posAmigoActualizado.latitud}, ${posAmigoActualizado.longitud}")

            val localfile = File. createTempFile( "tempImage", "jpg")
            val storageRef = FirebaseStorage.getInstance().reference.child("usuarios/${posAmigoActualizado.uid}.jpg")

            val markerOptions=MarkerOptions()
            storageRef.getFile(localfile).addOnSuccessListener {
                Log.i("fotoPin","ruta ${posAmigoActualizado.uid}.fotopin")
                var src = BitmapFactory.decodeFile(localfile.absolutePath)
                src=createCircledImage(src)
                val fotoAmigo = Bitmap.createScaledBitmap(src, 160, 160, true)
                //obtener foto de amigo y guardarlo en el mapa
                mapaDeFotoParticipantes[posAmigoActualizado.uid] = fotoAmigo
                markerOptions.position(LatLng(posAmigoActualizado.latitud, posAmigoActualizado.longitud)) // Establecer la posición del marcador
                    .title(posAmigoActualizado.nombre)
                    .icon(BitmapDescriptorFactory.fromBitmap(fotoAmigo))
                mapaDeParticipantes[posAmigoActualizado.uid]?.remove()
                val marker =mMap.addMarker(markerOptions) // Agregar el marcador al mapa

                // Agregar el marcador al mapa de participantes
                mapaDeParticipantes[posAmigoActualizado.uid] = marker
            }.addOnFailureListener{
                Log.i("revisar anadir", "no se pudo poner la foto del pin")
                markerOptions.position(LatLng(posAmigoActualizado.latitud, posAmigoActualizado.longitud)) // Establecer la posición del marcador
                    .title(posAmigoActualizado.nombre)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))

                mapaDeParticipantes[posAmigoActualizado.uid]?.remove()
                val marker =mMap.addMarker(markerOptions) // Agregar el marcador al mapa

                // Agregar el marcador al mapa de participantes
                mapaDeParticipantes[posAmigoActualizado.uid] = marker
            }
        }
    }
    private fun actualizarAmigo(dataSnapshot: DataSnapshot) {
        Log.i("amigo actualizado CHANGED","$dataSnapshot")
        val posAmigoActualizado = dataSnapshot.getValue(PosAmigo::class.java)
        if(posAmigoActualizado!=null)
        {
            Log.i("posAmigoActualizado CHANGED"," posAmigoActualizado: ${posAmigoActualizado.latitud}, ${posAmigoActualizado.longitud}")
            //actualizar pin del usuario en especifico
            val markerOptions = MarkerOptions()

            if(mapaDeFotoParticipantes[dataSnapshot.key]==null)
            {
                Log.e("ERROR CHANGED"," posAmigoActualizado: ${dataSnapshot.key}")

                markerOptions.position(LatLng(posAmigoActualizado.latitud, posAmigoActualizado.longitud)) // Establecer la posición del marcador
                    .title(posAmigoActualizado.nombre)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            }
            else
            {
                Log.i("CHANGED"," posAmigoActualizado: ${posAmigoActualizado.uid}")

                markerOptions.position(LatLng(posAmigoActualizado.latitud, posAmigoActualizado.longitud)) // Establecer la posición del marcador
                    .title(posAmigoActualizado.nombre)
                    .icon(mapaDeFotoParticipantes[dataSnapshot.key]?.let {
                        BitmapDescriptorFactory.fromBitmap(
                            it
                        )
                    })
            }

            // Agregar el marcador al mapa de participantes
            mapaDeParticipantes[posAmigoActualizado.uid]?.remove()

            val marker =mMap.addMarker(markerOptions) // Agregar el marcador al mapa

            mapaDeParticipantes[posAmigoActualizado.uid]=marker
        }
    }

    private fun subscribirseACambiosDePlan() {
        val databaseReference = databaseReferencePlanes.child(idPlan)

        //se va a escuchar el plan y se vera que se cambia
        //se evalua si se cambia algo de info del plan o tambien info de un usuario
        databaseReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {

            }
            override fun onChildChanged(dataSnapshot: DataSnapshot, prevChildKey: String?) {

                Log.i("snapshot PLAN"," pos: $dataSnapshot")
                if(dataSnapshot.key=="titulo")
                {
                    binding.tituloPlan.text= dataSnapshot.getValue().toString()
                }
                else if(dataSnapshot.key=="latitude")
                {
                    obtenerInfoCompletaPlan()
                }
                else if(dataSnapshot.key=="longitude")
                {
                    obtenerInfoCompletaPlan()
                }
                //revisar cambio de posicion del plan

            }
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }
            override fun onChildMoved(dataSnapshot: DataSnapshot, prevChildKey: String?) {
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }

    private fun obtenerInfoCompletaPlan() {
        // Realiza la consulta para obtener otros datos del usuario usando su UID
        // Por ejemplo, podrías usar una referencia a la base de datos para buscar los datos
        val planRef = database.getReference("Planes").child(idPlan)

        planRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Aquí puedes obtener los datos del usuario desde dataSnapshot
                val Plan = dataSnapshot.getValue(Plan::class.java)
                if (Plan != null) {
                    // Haz lo que necesites con los datos del usuario

                    longEncuentro=Plan.longitude
                    latEncuentro=Plan.latitude
                    ponerUbicacionPlan()

                } else {
                    println("No se encontraron datos para el usuario con UID: $idPlan")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Maneja el error en caso de que ocurra
                println("Error al obtener los datos del usuario: ${databaseError.message}")
            }
        })
    }

    private fun obtenerDatosInicialesAmigos(callback: (String) -> Unit) {

        var contador=0
        auth.currentUser?.let {
            databaseReferencePlanes.child(idPlan).child("integrantes")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        for (userSnapshot in dataSnapshot.children) {
                            val posId = userSnapshot.key
                            val posData = userSnapshot.getValue(PosAmigo::class.java)
                            if (posData != null && posId == auth.currentUser!!.uid) {
                                contador++
                            }
                            if (posData != null && posId != auth.currentUser!!.uid) {
                                val localfile = File.createTempFile("tempImage", "jpg")
                                val storageRef = FirebaseStorage.getInstance().reference.child("usuarios/${posData.uid}.jpg")

                                storageRef.getFile(localfile)
                                    .addOnSuccessListener { taskSnapshot ->
                                        Log.i("fotoPin inicial", "ruta ${posData.uid}.fotopin")
                                        var src = BitmapFactory.decodeFile(localfile.absolutePath)
                                        src = createCircledImage(src)
                                        val fotoAmigo = Bitmap.createScaledBitmap(src, 160, 160, true)

                                        val markerOptions = MarkerOptions()
                                            .position(LatLng(posData.latitud, posData.longitud))
                                            .title(posData.nombre)
                                            .icon(BitmapDescriptorFactory.fromBitmap(fotoAmigo))

                                        //val marker = mMap.addMarker(markerOptions)
                                        mapaDeParticipantes[posData.uid] = mMap.addMarker(markerOptions)
                                        contador++
                                        if(contador==dataSnapshot.childrenCount.toInt())
                                        {
                                            callback("listo")
                                        }

                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("Error de descarga", "Error al descargar la imagen para ${posData.nombre}: ${exception.message}")
                                        val markerOptions = MarkerOptions()
                                            .position(LatLng(posData.latitud, posData.longitud))
                                            .title(posData.nombre)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))

                                        val marker = mMap.addMarker(markerOptions)
                                        mapaDeParticipantes[posData.uid] = marker
                                        contador++

                                        if(contador==dataSnapshot.childrenCount.toInt())
                                        {
                                            callback("listo")
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