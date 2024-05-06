package com.example.primeraentrega.Adapters

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import com.example.primeraentrega.databinding.ContactrowBinding


class ContactsAdapter(context: Context?, c: Cursor?, flags: Int) :
    CursorAdapter(context, c, flags) {
    private lateinit var binding: ContactrowBinding

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        binding = ContactrowBinding.inflate(LayoutInflater.from(context))
        return binding.row
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val contactName = cursor?.getString(1)
        binding.contactName.text = contactName
    }


}