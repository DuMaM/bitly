package pl.nowak.bitly

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.ExpandableListView.OnChildClickListener
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
    private var devicesMap: HashMap<String, BluetoothDevice> = hashMapOf()

    private val multiplePermissions: Int = 100
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    private lateinit var chart: BarChart
    private lateinit var chartBER: BarChart

    val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
    val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

    private val mConnectionState: TextView? = null
    private val mDataField: TextView? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private val mGattServicesList: ExpandableListView? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private val mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null

    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Timber.e("Unable to initialize Bluetooth")
                finish()
            }

            mBluetoothLeService!!.startAdv()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true
                //updateConnectionState(R.string.connected)
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false
                //updateConnectionState(R.string.disconnected)
              //  clearUI()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
               // displayGattServices(mBluetoothLeService.getSupportedGattServices())
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
               // displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private val servicesListClickListner = OnChildClickListener { parent, v, groupPosition, childPosition, id ->
            if (mGattCharacteristics != null) {
                val characteristic: BluetoothGattCharacteristic =
                    mGattCharacteristics.get(groupPosition).get(childPosition)
                val charaProp = characteristic.properties
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService?.setCharacteristicNotification(mNotifyCharacteristic!!, false)
                        mNotifyCharacteristic = null
                    }
                    mBluetoothLeService?.readCharacteristic(characteristic)
                }
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    mNotifyCharacteristic = characteristic
                    mBluetoothLeService?.setCharacteristicNotification(characteristic, true)
                }
                return@OnChildClickListener true
            }
            false
        }

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

    private fun setData(count: Int, range: Double) : BarData {
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
        chart.setData(data)

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
        enableBle()
        initGraphs()

        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        disableBle()
    }

    private fun enableBle() {
        bluetoothManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

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
        var display: String = device.address
        if (device.name != null) {
            display += ": " + device.name
        }

        if (devicesMap.containsKey(display)) {
            return
        }

        spDevicesArray.add(display)
        devicesMap[display] = device
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            multiplePermissions -> {
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
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT), multiplePermissions)
            } else {
                Timber.d("Requesting permissions for app BLE location and admin")
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION), multiplePermissions)
            }
        }

        if ((ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN) +
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) +
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADVERTISE) +
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION))
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.BLUETOOTH_ADVERTISE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                permRequest()
            } else {
                permRequest()
            }
        }
    }

    private var scanning = false
    private val handler: Handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            addDeviceToView(result.getDevice())
        }
    }

    private fun scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(Runnable {
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }


    private fun initBleList() {
        // init device linear layout
        spDevicesArray = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ArrayList<String>()  )
        spDevices = findViewById(R.id.spDevicesView)
        spDevices.setAdapter(spDevicesArray)
        spDevices.setVisibility(View.VISIBLE)

        btScan = this.findViewById(R.id.btScanView)
        btScan.setOnClickListener {
            scanLeDevice()
//            Timber.i("Loading bounded devices on list")
//            bluetoothAdapter.bondedDevices.forEach { device ->
//                addDeviceToView(device)
//            }
//
//            // https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#startDiscovery()
//            Timber.i("Looking for devices")
//            if (!bluetoothAdapter.isDiscovering) {
//                bluetoothAdapter.startDiscovery()
//                Toast.makeText(this, "Scanning started", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "Scanning is still in progress", Toast.LENGTH_SHORT).show()
//            }
        }

        btClear = this.findViewById(R.id.btCleanView)
        btClear.setOnClickListener {
           // bluetoothAdapter.cancelDiscovery()
            bluetoothLeScanner.stopScan(leScanCallback)
            spDevicesArray.clear()
            devicesMap.clear()
        }

        btConnect = this.findViewById(R.id.btConnectView)
        btConnect.setOnClickListener {
            val deviceName: String = spDevices.getSelectedItem().toString()
            val device: BluetoothDevice? = devicesMap[deviceName]
            if (device != null) {
                // it's not allowed to discover and connect
                //bluetoothAdapter.cancelDiscovery()
                bluetoothLeScanner.stopScan(leScanCallback)
                mBluetoothLeService?.connect(device.address)

            }
        }
    }
}