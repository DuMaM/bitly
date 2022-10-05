package pl.nowak.bitly

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.nowak.bitly.databinding.ActivityMainBinding
import timber.log.Timber


class ActivityMain : AppCompatActivity() {
    // view
    private lateinit var binding: ActivityMainBinding
    private lateinit var textStatus: TextView

    // ecg recycle view
    private lateinit var ecgCharts: RecyclerView

    // service
    private val multiplePermissions: Int = 100
    private var permissionList = arrayOf(
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
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

    override fun onCreate(savedInstanceState: Bundle?) {
        // init activity
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init status text
        textStatus = binding.textConnectionStatus

        // recycle view
        ecgCharts = binding.recycleChartList
        val adapter = AdapterChartList()
        ecgCharts.adapter = adapter
        ecgCharts.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        checkBlePermission()

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            multiplePermissions -> {
                if (grantResults.isEmpty()) {
                    Timber.e("Not granted access")
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
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Please grant us ${permissions[index]}",
                            Toast.LENGTH_SHORT
                        )
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
            requestPermissions(permissionList, multiplePermissions)
        }

        permissionList.forEachIndexed { _, item: String ->
            if (ContextCompat.checkSelfPermission(
                    this@ActivityMain,
                    item
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@ActivityMain, item)) {
                    Toast.makeText(
                        applicationContext,
                        "Please allow for this permission ${item}",
                        Toast.LENGTH_SHORT
                    )
                } else {
                    permRequest()
                }
            }
        }
    }

    fun updateConnectionStatus(text: String) {
        runOnUiThread {
            textStatus.text = text.uppercase()
        }
    }

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    fun advertise() {
        mBluetoothLeService.startAdv()
    }

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    fun disconnect() {
        mBluetoothLeService.disconnectFromDevices()
    }

}
