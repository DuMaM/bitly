package pl.nowak.bitly

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.NonNull


// Adapter for holding devices found through scanning.
class LeDeviceListAdapter() : BaseAdapter() {
    private var mLeDevices: ArrayList<BluetoothDevice>
    private var mInflator: LayoutInflater? = null
    private var mContext: Context? = null

    init {
        mLeDevices = ArrayList()
        mInflator = null
        mContext = null
    }

    constructor(context: Context, resource: Int, objects: ArrayList<BluetoothDevice>) : this() {
        mLeDevices = objects
        mInflator = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mContext = context
    }

    fun addDevice(device: BluetoothDevice) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device)
        }
    }

    fun getDevice(position: Int): BluetoothDevice {
        return mLeDevices[position]
    }

    fun clear() {
        mLeDevices.clear()
    }

    override fun getCount(): Int {
        return mLeDevices.size
    }

    override fun getItem(i: Int): Any {
        return mLeDevices[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, view: View, viewGroup: ViewGroup): View {
        var view = view
        var viewHolder: ViewHolder
        // General ListView optimization code.
        if (view == null) {
            view = mInflator!!.inflate(R.layout.listitem_device, null)
            viewHolder = ViewHolder()
            viewHolder.deviceAddress = view.findViewById<View>(R.id.device_address) as TextView
            viewHolder.deviceName = view.findViewById<View>(R.id.device_name) as TextView
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        val device = mLeDevices[i]
        val deviceName = device.name
        if (deviceName != null && deviceName.length > 0) {
            viewHolder.deviceName?.setText(deviceName)
        } else {
            viewHolder.deviceName?.setText(R.string.unknown_device)
        }

        viewHolder.deviceAddress?.setText(device.address)
        return view
    }

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }
}

