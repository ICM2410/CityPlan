package com.example.primeraentrega

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.audiofx.BassBoost
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.primeraentrega.Adapters.GroupAdapter
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.primeraentrega.Clases.Grupo
import com.example.primeraentrega.Clases.ListGroup
import com.example.primeraentrega.databinding.ActivityVerGruposBinding
import com.example.primeraentrega.Clases.UsuarioAmigo
import com.example.primeraentrega.Services.NewPlanService
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.google.firebase.messaging.messaging


class VerGruposActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerGruposBinding
    private lateinit var database : FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerGruposBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        databaseReference= FirebaseDatabase.getInstance().getReference("Grupos")

        inicializarBotones()

        auth = FirebaseAuth.getInstance()
        //crearInfoSophie()
        llenarLista()

        binding.gruposList.setOnItemClickListener { parent, view, position, id ->
            val group = groupList[position] // Obtener el grupo clickeado de la lista
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("groupId", group.uid) // Pasar el ID del grupo a ChatActivity
            intent.putExtra("userId", auth.currentUser?.uid) // Pasar el ID del usuario actual a ChatActivity
            startActivity(intent)
        }

        //childId="-Nxds2b-dh--IP1NUNhP"
        //crearInfoSophie()
        gestionarPermiso()
        gestionarAlarma()
        iniciarServicio()
        configurarLocalizacion()

    }

    override fun onPause() {
        super.onPause()
        Log.e("PAUSA", "PAUSO ESTO")
        stopLlenarLista()
    }



    private fun stopLlenarLista() {
        // Remove the listener to stop receiving updates from Firebase
        databaseReference.removeEventListener(childEventListener!!)
    }

    private fun iniciarServicio() {
        Intent (applicationContext, NewPlanService::class.java). apply {
            action = NewPlanService.ACTION_START
            putExtra("uid", auth.currentUser?.uid)
            startService(this) // Aquí inicia el servicio
        }
    }

    private fun gestionarAlarma() {
        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        when {
            alarmManager.canScheduleExactAlarms() -> {
                Log.d("MainActivity", "onCreate: SCHEDULE ALARM")
            }
            else -> {
                // go to exact alarm settings
                Intent().apply {
                    action = ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }.also {
                    startActivity(it)
                }
            }
        }
    }

    private fun subscribirACanal(canal:String) {
        //aqui se debe subscribir a todos los chats a los que pertenece
        Firebase.messaging.subscribeToTopic(canal).addOnSuccessListener {
            Log.i("subscripcion", "Existosa")
        }.addOnFailureListener{
            Log.e("subscripcion", "ERROR")
        }
    }

    override fun onRestart() {
        super.onRestart()
        gestionarPermiso()
        llenarLista()
    }

    private val localPermissionName=android.Manifest.permission.ACCESS_FINE_LOCATION;
    private val notificationpermissionName=android.Manifest.permission.POST_NOTIFICATIONS
    private val ALARMpermissionName=android.Manifest.permission.SCHEDULE_EXACT_ALARM
    private val multiplepPermissionNameList= arrayOf(localPermissionName,notificationpermissionName)

    fun gestionarPermiso() {

        if (ContextCompat.checkSelfPermission(this, notificationpermissionName) == PackageManager.PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(this, localPermissionName) == PackageManager.PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(this, ALARMpermissionName) == PackageManager.PERMISSION_DENIED
        ) {

            if (shouldShowRequestPermissionRationale(notificationpermissionName)) {
                // Mostrar una explicación al usuario sobre por qué se necesitan los permisos de notificación
                Toast.makeText(applicationContext, "La aplicación necesita permisos para mostrar notificaciones y usar la localizacion", Toast.LENGTH_LONG).show()
            }
            // Solicitar permisos de notificación
            requestMultiplePermissions.launch(multiplepPermissionNameList)
        }
        else if (ContextCompat.checkSelfPermission(this, notificationpermissionName) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(notificationpermissionName)) {
                // Mostrar una explicación al usuario sobre por qué se necesitan los permisos de notificación
                Toast.makeText(applicationContext, "La aplicación necesita permisos para mostrar notificaciones", Toast.LENGTH_LONG).show()
            }
            // Solicitar permisos de notificación
            permissionRequestNotificacion.launch(notificationpermissionName)
        }
        else if(ContextCompat.checkSelfPermission(this, localPermissionName) == PackageManager.PERMISSION_DENIED) {
            if(shouldShowRequestPermissionRationale(localPermissionName))
            {
                Toast.makeText(getApplicationContext(), "The app requires access to location", Toast.LENGTH_LONG).show();
            }
            permissionRequest.launch(localPermissionName)
        }
        else if(ContextCompat.checkSelfPermission(this, ALARMpermissionName) == PackageManager.PERMISSION_DENIED) {
            if(shouldShowRequestPermissionRationale(ALARMpermissionName))
            {
                Toast.makeText(getApplicationContext(), "The app requires access to location", Toast.LENGTH_LONG).show();
            }
            permissionRequestAlarm.launch(ALARMpermissionName)
        }
        else {
            // La aplicación ya tiene permisos, mostrar notificaciones
            //notificar()
            startLocationUpdates()
            //LANZAR SERVICIO DE ALARMAS
        }
    }

    val permissionRequestNotificacion = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            if(it)
            {
                //notificar()
                Log.i("notification","notificaciones garantizadas")
            }
        }
    )

    val permissionRequestAlarm= registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            if(it)
            {
                Log.i("Alarm","Alarma CONCEDIDA")
            }
        })

    val permissionRequest= registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            if(it)
            {
                locationSettings()
            }
        })

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { entry ->
                val permission = entry.key
                val isGranted = entry.value
                // Manejar la respuesta de cada permiso individualmente
                if (permission == android.Manifest.permission.ACCESS_FINE_LOCATION) {
                    if (isGranted) {
                        // El permiso de ubicación fue concedido
                        locationSettings()
                    }
                } else if (permission == android.Manifest.permission.POST_NOTIFICATIONS) {
                    if (isGranted) {
                        // El permiso de notificaciones fue concedido
                        //notificar()
                        Log.i("notification","notificaciones garantizadas")
                    }
                }
                // Puedes manejar más permisos aquí si es necesario
            }
        }

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback
    lateinit var location: FusedLocationProviderClient

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
    fun startLocationUpdates()
    {
        if(ActivityCompat.checkSelfPermission(this, localPermissionName)== PackageManager.PERMISSION_GRANTED)
        {
            location.requestLocationUpdates(locationRequest,locationCallBack, Looper.getMainLooper())

            //PARA PONER LA POSICION INICIAL DEL USUARIO
            location.lastLocation.addOnSuccessListener {
                if (it != null) {
                    //latActual=it.latitude
                    //longActual=it.longitude
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

    private fun configurarLocalizacion() {

        location= LocationServices.getFusedLocationProviderClient(this);
        locationRequest=createLocationRequest()
        locationCallBack=createLocationCallback()

        //primero gestionar los permisos
        gestionarPermiso()

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
                    auth.currentUser?.uid?.let { userId ->
                        val refUsuario=FirebaseDatabase.getInstance().getReference("Usuario")
                        refUsuario.child(userId).apply {
                            child("latitud").setValue( last.latitude)
                            child("longitud").setValue( last.longitude)
                        }
                    }

                }
            }
        }

        return locationCallback
    }

    private fun createLocationRequest():LocationRequest
    {
        val request=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 7000)
            .setMinUpdateIntervalMillis(2000)
            .setWaitForAccurateLocation(true)
            .build()

        return request
    }

    private fun inicializarBotones() {
        val usuario=UsuarioAmigo()
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Grupos_bar -> {
                    // Respond to navigation item 1 click
                    //startActivity(Intent(baseContext, VerGruposActivity::class.java))

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
                    startActivity(Intent(baseContext, PerfilConfActivity::class.java))
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(baseContext, IniciarSesionActivity::class.java))
                    true
                }
                else -> false
            }
        }


        binding.botonAgregarGrupo.setOnClickListener {
            startActivity(Intent(baseContext, AgregarContactosActivity::class.java))
        }

    }

    val groupList: MutableList<ListGroup> = mutableListOf()
    private var childEventListener: ChildEventListener? = null
    private fun llenarLista() {

        databaseReference = FirebaseDatabase.getInstance().getReference("Groups")

        auth.currentUser?.uid?.let { currentUserUid ->
            childEventListener = databaseReference.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {

                    // Obtener el usuario de dataSnapshot
                    val grupo = dataSnapshot.getValue(Grupo::class.java)
                    Log.e("Referencia", "Aqui llegue a Grupo")
                    Log.e("GrupoImagen", "Imagen: ${grupo?.fotoGrupo}")
                    // Verificar si el usuario no es el usuario actual antes de agregarlo a la lista
                    if (grupo != null && grupo.integrantes.containsKey(currentUserUid)) {

                        Log.e("Referencia", "Apunto de pedir storageRef")
                        val storageRef = FirebaseStorage.getInstance().reference.child(grupo.fotoGrupo)
                        Log.e("Referencia", "Ya pedi")
                        val localfile = File. createTempFile( "tempImage", "jpg")

                        Log.e("GetFile", "Pedire local file")
                        storageRef.getFile(localfile).addOnSuccessListener {
                            Log.e("Entre", "ENTRE")
                            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                            var groupADD= ListGroup(grupo.titulo, dataSnapshot.key, bitmap)
                            groupList.add(groupADD)

                            //se subscribe al canal de notificaciones del grupo

                            //SUBSCRIPCION SE HACE AQUI
                            dataSnapshot.key?.let { it1 -> subscribirACanal(it1) }

                            //Lista
                            val adapter = GroupAdapter(applicationContext,groupList);
                            binding.gruposList.adapter = adapter

                        }.addOnFailureListener{
                            Log.e("Error", "User could not be found")
                        }

                    }
                }
                override fun onChildChanged(dataSnapshot: DataSnapshot, prevChildKey: String?) {

                }
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {

                }
                override fun onChildMoved(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                }
                override fun onCancelled(databaseError: DatabaseError) {
                }
            })
        }

    }

}