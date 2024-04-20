package com.example.primeraentrega

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
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
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date
import kotlin.math.min

class PlanActivity : AppCompatActivity() {

    private lateinit var binding : ActivityPlanBinding

    //PARA POSICIONES
    private var posActualGEO = GeoPoint(4.0, 72.0)
    private var latActual:Double= 4.0
    private var longActual:Double= 72.0
    private var posEncuentroGEO = GeoPoint(0.0, 0.0)
    private var latEncuentro:Double= 0.0
    private var longEncuentro:Double= 0.0
    private var pasosAvtivado=true
    private var EstoyEnElPlan=true
    private lateinit var roadManager: RoadManager
    private var firstTime=true

    private val localPermissionName=android.Manifest.permission.ACCESS_FINE_LOCATION;
    lateinit var location: FusedLocationProviderClient

    //MAPA
    lateinit var map : MapView
    private lateinit var geocoder: Geocoder

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

            //PARA PONER LA POSICION INICIAL DEL USUARIO
                /* location.lastLocation.addOnSuccessListener {
                if (it != null) {
                    latActual = it.latitude
                    longActual = it.longitude
                    posActualGEO = GeoPoint(it.latitude, it.longitude)
                    map.controller.setZoom(19.0)
                    map.controller.animateTo(posActualGEO)
                   if(EstoyEnElPlan)
                   {
                       myLocationOnMap(posActualGEO)
                   }
                }
            }*/
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
                    //Toast.makeText(getApplicationContext(), "($last.latitude , $last.longitude)", Toast.LENGTH_LONG).show();
                    latActual=last.latitude
                    longActual=last.longitude
                    posActualGEO=GeoPoint(latActual, longActual)
                    if(EstoyEnElPlan)
                    {
                        myLocationOnMap(posActualGEO)
                        if(firstTime)
                        {
                            firstTime=false
                            map.controller.animateTo(posActualGEO)
                            map.controller.setZoom(19.0)
                        }
                    }
                }
            }
        }

        return locationCallback
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarMapa()

        configurarLocalizacion()

        configurarConFireBase()

        configurarBotones();

        activarOMS()
    }

    private fun configurarConFireBase() {
        //AQUI SE OBTIENE LA INFORMACION PARA PONER
        //TITULO DEL PLAN
        var titulo="golf"
        binding.tituloPlan.setText(titulo)
        //UBICACION DEL PLAN
        posEncuentroGEO=GeoPoint(latEncuentro, longEncuentro)
        //SI TIENE LO DE NUMERO DE PASOS
        if(!pasosAvtivado){
            // Hacer invisible el elemento binding.hazDado
            binding.hazDado.visibility = View.INVISIBLE

            // Hacer invisible el elemento binding.pasoscantText
            binding.pasoscantText.visibility = View.INVISIBLE
        }

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
            binding.hazDado.visibility = View.INVISIBLE

            // Hacer invisible el elemento binding.pasoscantText
            binding.pasoscantText.visibility = View.INVISIBLE

            binding.mostrarRutabutton.isVisible= false
            binding.milocalizacion.isVisible=false
            stopLocationUpdates()
        }
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        map.controller.setZoom(19.0)
        map.controller.animateTo(posActualGEO)
        startLocationUpdates()
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
    private fun stopLocationUpdates() {
        location.removeLocationUpdates(locationCallBack)
    }

    private fun  configurarBotones()
    {
        binding.configuraciones.setOnClickListener{
            val intent=Intent(baseContext,EditarPlanActivity::class.java)
            startActivity(intent)
        }

        binding.botonCamara.setOnClickListener{
            startActivity(Intent(baseContext,GaleriaActivity::class.java))
        }

    }

    private var switchRuta=false
    private fun configurarLocalizacion() {

        location= LocationServices.getFusedLocationProviderClient(this);
        locationRequest=createLocationRequest()
        locationCallBack=createLocationCallback()

        //primero gestionar los permisos
        gestionarPermiso()

        ponerUbicacionPlan()

        binding.mostrarRutabutton.setOnClickListener{
            //muestra la ruta con oms bonus
            if(!switchRuta) {
                switchRuta=true
                binding.mostrarRutabutton.setText("Quitar ruta")
                mostrarRuta(posActualGEO, posEncuentroGEO)
            }
            else
            {
                switchRuta=false
                //quita la ruta si esta existe
                if(roadOverlay != null){
                    map.getOverlays().remove(roadOverlay);
                }
                binding.mostrarRutabutton.setText("Mostrar ruta")
            }
        }

        binding.puntoEncuentro.setOnClickListener{
            //lo centra a la ubicacion del punto de encuentro
            map.controller.setZoom(19.0)
            map.controller.animateTo(posEncuentroGEO)
        }

        binding.milocalizacion.setOnClickListener{
            //lo centra a la ubicacion de mi ubicacion actual
            //centrarse
            map.controller.setZoom(19.0)
            map.controller.animateTo(posActualGEO)
        }

        //ver si esta prendido o apagado
        binding.switchPasos.setOnClickListener {
            if(binding.switchPasos.isChecked)
            {
                if(pasosAvtivado)
                {
                    binding.hazDado.visibility = View.VISIBLE

                    // Hacer invisible el elemento binding.pasoscantText
                    binding.pasoscantText.visibility = View.VISIBLE
                }
                binding.milocalizacion.isVisible=true
                binding.mostrarRutabutton.isVisible= true
                binding.aunsiguesText.setText("Aun sigues en el plan")
                EstoyEnElPlan=true
                startLocationUpdates()
                myLocationOnMap(posActualGEO)
                map.controller.setZoom(19.0)
                map.controller.animateTo(posActualGEO)
            }
            else
            {

                binding.hazDado.visibility = View.INVISIBLE
                binding.milocalizacion.isVisible=false
                // Hacer invisible el elemento binding.pasoscantText
                binding.pasoscantText.visibility = View.INVISIBLE

                binding.mostrarRutabutton.isVisible= false
                binding.aunsiguesText.setText("Estas fuera del plan")
                stopLocationUpdates()
                if(myLocationMarker!=null)
                    map.overlays.remove(myLocationMarker)
                if(roadOverlay != null){
                    map.getOverlays().remove(roadOverlay);
                }
            }
        }
    }

    private fun ponerUbicacionPlan() {
        planLocationOnMap(posEncuentroGEO)
    }

    private fun createLocationRequest():LocationRequest
    {
        val request=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 7000)
            .setMinUpdateIntervalMillis(2000)
            .setWaitForAccurateLocation(true)
            .build()

        return request
    }


    //MARCADORES
    private var  myLocationMarker: Marker? = null
    fun myLocationOnMap(p: GeoPoint){
        if(myLocationMarker!=null)
            map.overlays.remove(myLocationMarker)
        val address = findAddress(LatLng(p.latitude, p.longitude))
        val snippet : String
        if(address!=null) {
            snippet = address
        }else{
            snippet = ""
        }
        addMarker(p, snippet, 0)
    }

    private var  planLocationMarker: Marker? = null
    fun planLocationOnMap(p: GeoPoint){
        if(planLocationMarker!=null)
            map.overlays.remove(planLocationMarker)
        val address = findAddress(LatLng(p.latitude, p.longitude))
        val snippet : String
        if(address!=null) {
            snippet = address
        }else{
            snippet = ""
        }
        addMarker(p, snippet, 1)
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
        if(tipo==0){
            myLocationMarker = createMarker(p, "Tu ubicacion", snippet, R.drawable.pinyo)

            if (myLocationMarker != null) {
                map.getOverlays().add(myLocationMarker)
            }
        }
        else //localizacion plan
        {
            planLocationMarker = createMarkerEncuentro(p, "Tu destino", snippet, R.drawable.iconopin)

            if (planLocationMarker != null) {
                map.getOverlays().add(planLocationMarker)
            }
        }
    }

    val sizeInDp = 70
    //val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDp.toFloat(), resources.displayMetrics).toInt()
    @SuppressLint("UseCompatLoadingForDrawables")
    fun createMarkerEncuentro(p: GeoPoint, title: String, desc: String, iconID : Int) : Marker? {
        var marker : Marker? = null;
        if(map!=null) {
            marker = Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                val MAX_ICON_SIZE = 200
                Thread(Runnable {
                    val originalBitmap = BitmapFactory.decodeResource(resources, iconID)

                    // Redimensionar la imagen manteniendo la forma circular
                    val resizedBitmap = if (originalBitmap.width > MAX_ICON_SIZE || originalBitmap.height > MAX_ICON_SIZE) {
                        val maxSize = min(originalBitmap.width, originalBitmap.height)
                        val scaleFactor = MAX_ICON_SIZE.toFloat() / maxSize
                        val scaledWidth = (originalBitmap.width * scaleFactor).toInt()
                        val scaledHeight = (originalBitmap.height * scaleFactor).toInt()
                        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

                        // Recortar el bitmap para mantener la forma circular
                        val outputBitmap = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(outputBitmap)
                        val paint = Paint().apply {
                            isAntiAlias = true
                            shader = BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                        }
                        val diameter = min(scaledBitmap.width, scaledBitmap.height).toFloat()
                        canvas.drawCircle(scaledBitmap.width / 2f, scaledBitmap.height / 2f, diameter / 2, paint)

                        outputBitmap
                    } else {
                        val maxSize = min(originalBitmap.width, originalBitmap.height)
                        val scaleFactor = MAX_ICON_SIZE.toFloat() / maxSize
                        val scaledWidth = (originalBitmap.width * scaleFactor).toInt()
                        val scaledHeight = (originalBitmap.height * scaleFactor).toInt()
                        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

                        // Recortar el bitmap para mantener la forma circular
                        val outputBitmap = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(outputBitmap)
                        val paint = Paint().apply {
                            isAntiAlias = true
                            shader = BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                        }
                        val diameter = min(scaledBitmap.width, scaledBitmap.height).toFloat()
                        canvas.drawCircle(scaledBitmap.width / 2f, scaledBitmap.height / 2f, diameter / 2, paint)

                        outputBitmap
                    }

                    // Comprimir y reducir la calidad de la imagen
                    val compressedBitmapStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, compressedBitmapStream) // Aquí puedes ajustar el nivel de compresión (0-100)

                    // Convertir el bitmap comprimido en un BitmapDrawable
                    val compressedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(compressedBitmapStream.toByteArray()))
                    val drawable = BitmapDrawable(resources, compressedBitmap)

                    // Establecer el BitmapDrawable como el icono del Marker
                    marker.setIcon(drawable)

                }).start()
            }
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

    fun createMarker(p: GeoPoint, title: String, desc: String, iconID : Int) : Marker? {
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
    private fun scaleBitmap(bitmap: Bitmap, size: Int): Bitmap {
        val scale = size.toFloat() / bitmap.width.toFloat()
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.isAntiAlias = true
        paint.shader = shader
        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, (bitmap.width / 2).toFloat(), paint)
        return output
    }

    //MAPA
    private fun configurarMapa() {
        Configuration.getInstance().load(this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        geocoder = Geocoder(baseContext)
        map = binding.osmMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        //map.overlays.add(createOverlayEvents())
    }


    //OMS Y CREAR RUTA
    private fun activarOMS() {
        roadManager = OSRMRoadManager(this, "ANDROID")
    }

    private var roadOverlay: Polyline? = null
    private fun mostrarRuta(start : GeoPoint, finish : GeoPoint){

        Thread(Runnable {var routePoints = ArrayList<GeoPoint>()
            routePoints.add(start)
            routePoints.add(finish)
            val road = roadManager.getRoad(routePoints)
            Log.i("MapsApp", "Route length: "+road.mLength+" km")
            Log.i("MapsApp", "Duration: "+road.mDuration/60+" min")
            if(map!=null){
                if(roadOverlay != null){
                    map.getOverlays().remove(roadOverlay);
                }
                roadOverlay = RoadManager.buildRoadOverlay(road)
                roadOverlay!!.getOutlinePaint().setColor(
                    Color.
                    RED)
                roadOverlay!!.getOutlinePaint().setStrokeWidth(10F)
                map.getOverlays().add(roadOverlay)
            }

        }).start()
    }
}