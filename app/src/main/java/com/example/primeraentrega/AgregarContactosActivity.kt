package com.example.primeraentrega

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.primeraentrega.adapters.ContactsAdapter
import com.example.primeraentrega.databinding.ActivityAgregarContactosBinding

class AgregarContactosActivity : AppCompatActivity() {

    //Permission val
    val getContactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            updateUI(it)
        })

    private lateinit var binding: ActivityAgregarContactosBinding
    private lateinit var adapter : ContactsAdapter
    val projection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgregarContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = ContactsAdapter(this, null, 0)
        binding.listaContactos.adapter=adapter
        permissionRequest()
    }

    fun permissionRequest(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED){
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)){
                Toast.makeText(this, "The app requires access to the contacts", Toast.LENGTH_LONG).show()
            }
            getContactsPermission.launch(android.Manifest.permission.READ_CONTACTS)
        }else{
            updateUI(true)
        }
    }

    fun updateUI(contacts : Boolean){
        if(contacts){
            //Permission Granted
            var cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null)
            adapter.changeCursor(cursor)

        }else {
            //Permission Denied

        }
    }
}