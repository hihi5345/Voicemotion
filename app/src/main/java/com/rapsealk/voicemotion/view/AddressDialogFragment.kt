package com.rapsealk.voicemotion.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.EditText
import com.rapsealk.voicemotion.R

/**
 * Created by rapsealk on 2018. 6. 5..
 */
 class AddressDialogFragment : DialogFragment() {

    public interface DialogListener {
        public fun onDialogPositiveClick(dialog: AddressDialogFragment, address: String)
        public fun onDialogNegativeClick(dialog: AddressDialogFragment)
    }

    private lateinit var mListener: DialogListener
    private lateinit var mIpAddress: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mIpAddress = getString(R.string.default_ip_address)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // return super.onCreateDialog(savedInstanceState)
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.dialog_ip_address, null)
        val etIpAddress = view.findViewById<EditText>(R.id.et_ip_address)
        etIpAddress.setText(mIpAddress)
        builder.setTitle(R.string.message_ip_address)
                .setView(view)
                .setPositiveButton(R.string.btn_register, { _, _ ->
                    mIpAddress = etIpAddress.text.toString()
                    mListener.onDialogPositiveClick(this, mIpAddress)
                })
                .setNegativeButton(R.string.btn_cancel, { _, _ ->
                    mListener.onDialogNegativeClick(this)
                })
        return builder.create()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            mListener = activity as DialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement DialogListener.")
        }
    }
}