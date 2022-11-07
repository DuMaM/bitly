package pl.nowak.bitly.repository

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.nowak.bitly.ble.BluetoothLeService
import pl.nowak.bitly.database.LeadDatabase
import pl.nowak.bitly.ecg.EcgData
import timber.log.Timber


class EcgDataRepository(private val database: LeadDatabase, val application: Application) {

    private lateinit var mBluetoothLeService: BluetoothLeService

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            Timber.i("repo is connected with ble service")
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Timber.i("repo is disconnected from ble service")
        }
    }

    init {
        val gattServiceIntent = Intent(application.applicationContext, BluetoothLeService::class.java)
        application.bindService(gattServiceIntent, mServiceConnection, AppCompatActivity.BIND_AUTO_CREATE)

    }

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    fun advertise() {
        if (this::mBluetoothLeService.isInitialized) {
            mBluetoothLeService.startAdv()
        } else {
            Timber.e("BLE service is not init yet")
        }
    }

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    fun disconnect() {
        mBluetoothLeService.disconnectFromDevices()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE"])
    suspend fun getData(): Flow<EcgData>? {
        if (ActivityCompat.checkSelfPermission(
                application.applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            delay(1000L)
            return null
        }

        if (!this@EcgDataRepository::mBluetoothLeService.isInitialized) {
            delay(1000L)
            return null
        }

        var ecgData: Flow<EcgData> = withContext(Dispatchers.IO) {
            mBluetoothLeService.mBluetoothServerFlow().map { data -> EcgData.loadData(data) }
        }
        return ecgData
    }
}