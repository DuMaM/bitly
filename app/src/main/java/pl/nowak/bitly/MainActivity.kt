package pl.nowak.bitly

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var btScan: Button
    private lateinit var btClear: Button
    private lateinit var btConnect: Button

    private lateinit var spDevices: Spinner
    private lateinit var spDevicesArray: ArrayAdapter<String>

    private val multiplePermissions: Int = 100

    private lateinit var chart: BarChart
    private lateinit var chartBER: BarChart

    private lateinit var mBluetoothLeService: BluetoothLeService

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService.initialize()) {
                Timber.e("Unable to initialize Bluetooth")
                finish()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
        }
    }

    private fun setData(count: Int, range: Double): BarData {
        // now in hours
        val now: Long = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis())
        val values: ArrayList<BarEntry> = ArrayList()

        // count = hours
        val to = (now + count).toFloat()

        // increment by 1 hour
        var x = now.toFloat()
        while (x < to) {
            val y: Double = Random.nextDouble(range, 50.0)
            values.add(BarEntry(x, y.toFloat())) // add one entry per hour
            x++
        }

        // create a dataset and give it a type
        val set1 = BarDataSet(values, "DataSet 1")
        set1.axisDependency = AxisDependency.LEFT
        set1.color = ColorTemplate.getHoloBlue()
        set1.valueTextColor = ColorTemplate.getHoloBlue()
        set1.setDrawValues(false)
        set1.highLightColor = Color.rgb(244, 117, 117)

        // create a data object with the data sets
        val data = BarData(set1)
        data.setValueTextColor(Color.WHITE)
        data.setValueTextSize(9f)

        // set data
        return data
    }

    private fun initGraphs() {

        // in this example, a LineChart is initialized from xml
        chart = findViewById<View>(R.id.chartRX) as BarChart

        // enable touch gestures
        chart.setTouchEnabled(true)

        // enable scaling and dragging
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.isHighlightPerDragEnabled = true

        // set an alternative background color
        val data = setData(20, 0.4)
        chart.data = data

        // in this example, a LineChart is initialized from xml
        chartBER = findViewById<View>(R.id.chartBER) as BarChart

        // enable touch gestures
        chartBER.setTouchEnabled(true)

        // enable scaling and dragging
        chartBER.isDragEnabled = true
        chartBER.setScaleEnabled(true)
        chartBER.isHighlightPerDragEnabled = true

        // set an alternative background color
        chartBER.data = setData(30, 0.3)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initGraphs()
    }

    override fun onResume() {
        super.onResume()
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        initBleList()

        checkBlePermission()
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    private fun addDeviceToView(address: String, name: String): Boolean {
        var display: String = address
        if (name.isNotEmpty()) {
            display += ": $name"
        }

        runOnUiThread { spDevicesArray.add(display) }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            multiplePermissions -> {
                if (grantResults.isEmpty()) {
                    Timber.w("Not granted access")
                    return
                }
                grantResults.forEachIndexed { index, it ->
                    if (it == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            permissions[index]
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
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
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ), multiplePermissions
                )
            } else {
                Timber.d("Requesting permissions for app BLE location and admin")
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), multiplePermissions
                )
            }
        }

        if ((ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_ADMIN
            ) +
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) +
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    ) +
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) +
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                permRequest()
            } else {
                permRequest()
            }
        }
    }


    private fun initBleList() {
        // init device linear layout
        spDevicesArray =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ArrayList<String>())
        spDevices = findViewById(R.id.spDevicesView)
        spDevices.adapter = spDevicesArray
        spDevices.visibility = View.VISIBLE

        btScan = this.findViewById(R.id.btScanView)
        btScan.setOnClickListener {
            mBluetoothLeService.startAdv()
        }

        btClear = this.findViewById(R.id.btCleanView)
        btClear.setOnClickListener {
            // bluetoothAdapter.cancelDiscovery()
            spDevicesArray.clear()
        }

        btConnect = this.findViewById(R.id.btConnectView)
        btConnect.setOnClickListener {
            val deviceName: String = spDevices.selectedItem.toString()
        }
    }
}