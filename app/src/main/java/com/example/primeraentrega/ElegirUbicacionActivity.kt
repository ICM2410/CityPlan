package com.example.primeraentrega

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.primeraentrega.databinding.ActivityCrearPlanBinding
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
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

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
                    map.controller.animateTo(posActualGEO)
                    map.controller.setZoom(19.0)
                    selectedLocationOnMap(posActualGEO)
                    location.removeLocationUpdates(locationCallBack)
                }
            }
        }

        return locationCallback
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityElegirUbicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //OBTENER INFORMACION DE LA OTRA PANTALLA DE LA UBICACION QUE SE QUIERE

        //SINO EXISTE INFORMACION QUE MANDO DICHA PANTALLA, SE PONE LA UBICACION ACTUAL

        inicializarBotones()

        configurarMapa()

        configurarLocalizacion()

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
            startActivity(Intent(baseContext, RecomendacionesActivity::class.java))
        }

        binding.guardar.setOnClickListener {
            startActivity(Intent(baseContext, CrearPlanActivity::class.java))
        }

    }

    private fun configurarLocalizacion() {

        location= LocationServices.getFusedLocationProviderClient(this);
        locationRequest=createLocationRequest()
        locationCallBack=createLocationCallback()

        //primero gestionar los permisos
        gestionarPermiso()
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
        selectedLocationMarker = createMarker(p, "Ubicacion seleccionada", snippet, R.drawable.foto2)

        if (selectedLocationMarker != null) {
            map.getOverlays().add(selectedLocationMarker)
        }
    }

    val sizeInDp = 70
    //val sizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDp.toFloat(), resources.displayMetrics).toInt()
    fun createMarker(p: GeoPoint, title: String, desc: String, iconID : Int) : Marker? {
        var marker : Marker? = null;
        if(map!=null) {
            marker = Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                //val myIcon = getResources().getDrawable(iconID, this.getTheme())
                //val scaledBitmap = scaleBitmap((myIcon as BitmapDrawable).bitmap, sizeInPixels)
                //val circularBitmap = getCircularBitmap(scaledBitmap)
                //val circularDrawable = BitmapDrawable(getResources(), circularBitmap)
                //marker.setIcon(circularDrawable)
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

}