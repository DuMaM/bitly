package pl.nowak.bitly

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var btScan: Button
    private lateinit var slDevices: LinearLayout
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var chart: LineChart
    private var devicesMap: HashMap<Int, BluetoothDevice> = hashMapOf<Int, BluetoothDevice>()


    private val MULTIPLE_PERMISSIONS = 100

    // add async pulling of discovery requests
    // https://developer.android.com/guide/topics/connectivity/bluetooth/find-bluetooth-devices
    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    addDeviceToView(device)
                }
            }
        }
    }

    private fun setData(count: Int, range: Double) {
        // now in hours
        val now: Long = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis())
        val values: ArrayList<Entry> = ArrayList()

        // count = hours
        val to = (now + count).toFloat()

        // increment by 1 hour
        var x = now.toFloat()
        while (x < to) {
            val y: Double = Random.nextDouble(range, 50.0)
            values.add(Entry(x, y.toFloat())) // add one entry per hour
            x++
        }

        // create a dataset and give it a type
        val set1 = LineDataSet(values, "DataSet 1")
        set1.axisDependency = AxisDependency.LEFT
        set1.color = ColorTemplate.getHoloBlue()
        set1.valueTextColor = ColorTemplate.getHoloBlue()
        set1.lineWidth = 1.5f
        set1.setDrawCircles(false)
        set1.setDrawValues(false)
        set1.fillAlpha = 65
        set1.fillColor = ColorTemplate.getHoloBlue()
        set1.highLightColor = Color.rgb(244, 117, 117)
        set1.setDrawCircleHole(false)

        // create a data object with the data sets
        val data = LineData(set1)
        data.setValueTextColor(Color.WHITE)
        data.setValueTextSize(9f)

        // set data
        chart.setData(data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        enableBle()

        // in this example, a LineChart is initialized from xml
        chart = findViewById<View>(R.id.chartRX) as LineChart

        // no description text
        chart.getDescription().setEnabled(false);

        // enable touch gestures
        chart.setTouchEnabled(true);

        chart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        chart.setBackgroundColor(Color.WHITE);
        chart.setViewPortOffsets(0f, 0f, 0f, 0f);

        setData(20, 0.3)

    }

    override fun onDestroy() {
        super.onDestroy()
        disableBle()
    }

    private fun enableBle() {
        bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        Timber.i("There is possibility to use bluetooth")

        checkBlePermission()

        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
            Timber.i("Enabling bluetooth")
        } else {
            Timber.i("Bluetooth is working")
        }

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        initBleList()
    }

    private fun disableBle() {
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }


    private fun addDeviceToView(device: BluetoothDevice) {
        if (devicesMap.containsKey(device.hashCode())) {
            return
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        params.setMargins(8, 8, 8, 8)

        val textView = TextView(this)
        textView.layoutParams = params
        textView.text = device.name + ":" + device.address

        devicesMap[device.hashCode()] = device
        slDevices.addView(textView, params)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MULTIPLE_PERMISSIONS -> {
                if (grantResults.isEmpty()) {
                    Timber.w("Not granted access")
                    return
                }
                grantResults.forEachIndexed { index, it ->
                    if (it == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this@MainActivity, permissions[index]) == PackageManager.PERMISSION_GRANTED) {
                            Timber.i("${permissions[index]} Granted access")

                    }
                }
                return
            }
        }
    }

    // function to check permissions
    private fun checkBlePermission() {
        val permRequest = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Timber.d("Requesting permissions for app BLE connect, scan, location, admin")
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT), MULTIPLE_PERMISSIONS)
            } else {
                Timber.d("Requesting permissions for app BLE location and admin")
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION), MULTIPLE_PERMISSIONS)
            }
        }

        if ((ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN) +
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) +
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION))
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                permRequest()
            } else {
                permRequest()
            }
        }
    }

    private fun initBleList() {
        slDevices = findViewById(R.id.devicesHolder_Linear)
        slDevices.removeAllViews()
        devicesMap.clear()

        Timber.i("Loading bounded devices on list")
        bluetoothAdapter.bondedDevices.forEach { device ->
            addDeviceToView(device)
        }

        btScan = this.findViewById(R.id.scan_Button)
        btScan.setOnClickListener {
            Toast.makeText(this, "Scanning started", Toast.LENGTH_LONG).show()

            // https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#startDiscovery()
            bluetoothAdapter.cancelDiscovery()
            if (bluetoothAdapter.startDiscovery()) {
                Timber.i("Looking for devices")
            }
        }
    }
}