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
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import pl.nowak.bitly.LeadName
import pl.nowak.bitly.ble.BluetoothLeService
import pl.nowak.bitly.database.LeadDatabase
import pl.nowak.bitly.database.LeadEntry
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

    suspend fun refreshData() {
        val gattServiceIntent = Intent(application.applicationContext, BluetoothLeService::class.java)
        application.bindService(gattServiceIntent, mServiceConnection, AppCompatActivity.BIND_AUTO_CREATE)

        if (ActivityCompat.checkSelfPermission(
                application.applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            delay(1000L)
            return
        }

        if (!this::mBluetoothLeService.isInitialized) {
            delay(1000L)
            return
        }

        withContext(Dispatchers.IO) {
            mBluetoothLeService.mBluetoothServerFlow().collect { data ->
                Timber.v(data.contentToString())

                val ecgData: EcgData = EcgData.loadData(data)

                ecgData.apply {
                    val timestamp = System.currentTimeMillis().toFloat()
                    database.leadDao.insert(
                        LeadEntry(x = v1_c1.toFloat(), y = timestamp, lead = LeadName.LeadV1.ordinal),
                        LeadEntry(x = v2_c2.toFloat(), y = timestamp, lead = LeadName.LeadV2.ordinal),
                        LeadEntry(x = v3_c3.toFloat(), y = timestamp, lead = LeadName.LeadV3.ordinal),
                        LeadEntry(x = v4_c4.toFloat(), y = timestamp, lead = LeadName.LeadV4.ordinal),
                        LeadEntry(x = v5_c5.toFloat(), y = timestamp, lead = LeadName.LeadV5.ordinal),
                        LeadEntry(x = v6_c6.toFloat(), y = timestamp, lead = LeadName.LeadV6.ordinal),
                        LeadEntry(x = lead1.toFloat(), y = timestamp, lead = LeadName.LeadI.ordinal),
                        LeadEntry(x = lead2.toFloat(), y = timestamp, lead = LeadName.LeadII.ordinal),
                        LeadEntry(x = lead3.toFloat(), y = timestamp, lead = LeadName.LeadIII.ordinal),
                        LeadEntry(x = aVF.toFloat(), y = timestamp, lead = LeadName.LeadAVL.ordinal),
                        LeadEntry(x = aVR.toFloat(), y = timestamp, lead = LeadName.LeadAVR.ordinal),
                        LeadEntry(x = aVF.toFloat(), y = timestamp, lead = LeadName.LeadAVF.ordinal)
                    )
                }
            }
        }
    }

    private val leadDataSize = 60

    val leadV1: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV1.ordinal, leadDataSize)
    val leadV2: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV2.ordinal, leadDataSize)
    val leadV3: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV3.ordinal, leadDataSize)
    val leadV4: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV4.ordinal, leadDataSize)
    val leadV5: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV5.ordinal, leadDataSize)
    val leadV6: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV6.ordinal, leadDataSize)
    val leadI: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadI.ordinal, leadDataSize)
    val leadII: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadII.ordinal, leadDataSize)
    val leadIII: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadIII.ordinal, leadDataSize)
    val leadAVL: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadAVL.ordinal, leadDataSize)
    val leadAVR: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadAVR.ordinal, leadDataSize)
    val leadAVF: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadAVF.ordinal, leadDataSize)
}