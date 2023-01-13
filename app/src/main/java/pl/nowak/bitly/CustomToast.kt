package pl.nowak.bitly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast


class CustomToast(private val context: Context, private val message: String) : Toast(context) {
    init {
        val view: View = LayoutInflater.from(context).inflate(R.layout.customtoast, null)
        val txtMsg = view.findViewById<TextView>(R.id.toasttext)
        txtMsg.text = message
        setView(view)
        duration = LENGTH_LONG
    }
}
