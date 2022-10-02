package pl.nowak.bitly

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import pl.nowak.bitly.databinding.ActivityMainBinding
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class ActivityMain : AppCompatActivity() {
    // view
    private lateinit var binding: ActivityMainBinding

    // view labels
    private lateinit var btAdv: Button
    private lateinit var btDrop: Button
    private lateinit var textStatus: TextView

    // view charts - big
    private lateinit var chart: BarChart
    private lateinit var chartBER: BarChart

    // service
    private val multiplePermissions: Int = 100
    private lateinit var mBluetoothLeService: BluetoothLeService

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService.initialize(this@ActivityMain::updateConnectionStatus)) {
                Timber.e("Unable to initialize Bluetooth")
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
        chart = binding.chartRX

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
        chartBER = binding.chartBER

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
        // init activity
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkBlePermission()
        setContentView(R.layout.activity_main)

        // init status text
        textStatus = binding.textConnectionStatus

        initGraphs()
    }

    override fun onResume() {
        super.onResume()
        checkBlePermission()

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        initBleList()
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
                            this@ActivityMain,
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
        }

        if ((ContextCompat.checkSelfPermission(
                this@ActivityMain,
                Manifest.permission.BLUETOOTH_ADMIN
            ) +
                    ContextCompat.checkSelfPermission(
                        this@ActivityMain,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) +
                    ContextCompat.checkSelfPermission(
                        this@ActivityMain,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    ) +
                    ContextCompat.checkSelfPermission(
                        this@ActivityMain,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) +
                    ContextCompat.checkSelfPermission(
                        this@ActivityMain,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@ActivityMain,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this@ActivityMain,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this@ActivityMain,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this@ActivityMain,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                permRequest()
            }
        }
    }

    fun updateConnectionStatus(text: String) {
        runOnUiThread {
            textStatus.text = text.uppercase()
        }
    }

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    private fun initBleList() {
        btAdv = binding.btAdvView
        btAdv.setOnClickListener {
            mBluetoothLeService.startAdv()
        }

        btDrop = binding.btDropView
        btDrop.setOnClickListener {
            // bluetoothAdapter.cancelDiscovery()
            mBluetoothLeService.disconnectFromDevices()
        }
    }
}