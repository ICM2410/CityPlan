package com.example.primeraentrega

import android.annotation.SuppressLint
import android.content.ContentValues
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
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.Counters
import org.osmdroid.tileprovider.util.StorageUtils
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.min

class ElegirUbicacionActivity : AppCompatActivity() {

    private lateinit var binding : ActivityElegirUbicacionBinding

    //PARA POSICIONES
    private var posActualGEO = GeoPoint(4.0, 72.0)
    private var latActual:Double= 4.0
    private var longActual:Double= 72.0

    private val localPermissionName=android.Manifest.permission.ACCESS_FINE_LOCATION;
    lateinit var location: FusedLocationProviderClient

    //MAPA
    lateinit var map : MapView
    private lateinit var geocoder: Geocoder

    //LOCALIZACION
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback

    private lateinit var bitmap:Bitmap

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
                        //Toast.makeText(getApplicationContext(), "($last.latitude , $last.longitude)", Toast.LENGTH_LONG).show();
                        latActual=last.latitude
                        longActual=last.longitude
                        posActualGEO=GeoPoint(latActual, longActual)


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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityElegirUbicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //OBTENER INFORMACION DE LA OTRA PANTALLA DE LA UBICACION QUE SE QUIERE

        //SINO EXISTE INFORMACION QUE MANDO DICHA PANTALLA, SE PONE LA UBICACION ACTUAL
        pantalla= intent.getStringExtra("pantalla").toString()

        inicializarBotones()

        inicializarImagen()

        configurarMapa()

        configurarLocalizacion()
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

    private fun inicializarImagen() {
        Thread(Runnable {
                val byteArray = intent.getByteArrayExtra("pinImage")
                if (byteArray != null) {
                    bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

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
        }).start()

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
                    selectedLocationOnMap(p)
                }
                return true
            }
        })
        return overlayEvents
    }

    private fun inicializarBotones() {
        binding.verRecomendacion.setOnClickListener {
            //clearOsmdroidTileCache()
            val intent=Intent(baseContext, RecomendacionesActivity::class.java)
            intent.putExtra("pantalla",pantalla)
            startActivity(intent)
        }
        binding.guardar.setOnClickListener {
            //aqui se va a devolver la posicion del plan en long y lat

            var intent = Intent(baseContext, EditarPlanActivity::class.java)

            if(pantalla=="crear")
            {
                intent = Intent(baseContext, CrearPlanActivity::class.java)
            }

            val stream = ByteArrayOutputStream()
            // Agregar los valores de longitud y latitud como extras al Intent
            intent.putExtra("longitud", longActual)
            intent.putExtra("latitud", latActual)

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            intent.putExtra("pinImage", byteArray)

            // Iniciar la siguiente actividad con el Intent modificado
            startActivity(intent)
        }
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
        selectedLocationMarker = createMarker(p, "Ubicacion seleccionada", snippet, bitmap)

        if (selectedLocationMarker != null) {
            map.getOverlays().add(selectedLocationMarker)
        }
    }

    val sizeInDp = 70
    //val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDp.toFloat(), resources.displayMetrics).toInt()
    @SuppressLint("SuspiciousIndentation")
    fun createMarker(p: GeoPoint, title: String, desc: String, iconID: Bitmap) : Marker? {
        var marker : Marker? = null;
        if(map!=null) {
            marker = Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
                val MAX_ICON_SIZE = 130
                Thread(Runnable {
                    val originalBitmap = iconID

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

}