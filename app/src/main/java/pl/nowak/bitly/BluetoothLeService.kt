package pl.nowak.bitly

import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import timber.log.Timber
import java.util.*


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class BluetoothLeService : Service() {
    private val mBluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val mBluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val mBluetoothLEScanner: BluetoothLeScanner by lazy {
        mBluetoothAdapter.bluetoothLeScanner
    }
    private val mBluetoothAdvertiser: BluetoothLeAdvertiser by lazy {
        mBluetoothAdapter.bluetoothLeAdvertiser
    }

    private val mBluetoothGattServer: BluetoothGattServer by lazy {
        mBluetoothManager.openGattServer(this, bluetoothGattServerCallback)
    }

    private var mBluetoothGattService: BluetoothGattService = BluetoothGattService(UUID.fromString(UUID_THROUGHPUT_MEASUREMENT), BluetoothGattService.SERVICE_TYPE_PRIMARY)


    private val mBleDataResponseResponse: AdvertiseData = AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .build()

    private var currentAdvertisingSet: AdvertisingSet? = null
    private val mBluetoothAdvParameters: AdvertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
        .setConnectable(true)
        .build()
    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent): IBinder? {
        if (!isEnabled()) {
            mBluetoothAdapter.enable()
            Timber.i("Enabling bluetooth")
        } else {
            Timber.i("Bluetooth is working")
        }


        val characteristic = BluetoothGattCharacteristic(UUID.fromString(UUID_THROUGHPUT_MEASUREMENT_CHAR),
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        mBluetoothGattService.addCharacteristic(characteristic)
        mBluetoothGattServer.addService(mBluetoothGattService)

        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        return super.onUnbind(intent)
    }


    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        if (!isEnabled()) {
            enableBle()
        }

        return true
    }

    fun isEnabled(): Boolean {
        if (!mBluetoothAdapter.isEnabled) {
            Timber.i("Bluetooth is not enabled")
        } else {
            Timber.i("Bluetooth is working")
        }
        return mBluetoothAdapter.isEnabled
    }

    fun startAdv() : Boolean {
        if (isMultipleAdvertisementSupported()) {
            Timber.i("Multiple advertisement supported")
        } else {
            Timber.i("Multiple advertisement not supported")
            return false
        }

        // After onAdvertisingSetStarted callback is called, you can modify the
        // advertising data and scan response data:
        val pUuid = ParcelUuid(UUID.fromString(UUID_THROUGHPUT_MEASUREMENT))
        val mBleData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(pUuid)
            .build()

        val isNameChanged = BluetoothAdapter.getDefaultAdapter().setName("Nordic_Performance_Test")
        if (isNameChanged) {
            Timber.i("Name changed")
        } else {
            Timber.i("Name not changed")
        }
        mBluetoothAdvertiser.startAdvertising(mBluetoothAdvParameters, mBleData,mBleDataResponseResponse, mAdvCallback)
        return true
    }

    fun stopAdv() {
        // Can also stop and restart the advertising
        currentAdvertisingSet?.enableAdvertising(false, 0, 0)
        // Wait for onAdvertisingEnabled callback...
        currentAdvertisingSet?.enableAdvertising(true, 60000, 200)
        // Wait for onAdvertisingEnabled callback...

        // Wait for onScanResponseDataSet callback...
        mBluetoothAdvertiser.stopAdvertising(mAdvCallback)
    }

    private val mAdvCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Timber.i("LE Advertise success.")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> Timber.e("LE Advertise failed: ADVERTISE_FAILED_DATA_TOO_LARGE")
                ADVERTISE_FAILED_ALREADY_STARTED -> Timber.e("LE Advertise failed: ADVERTISE_FAILED_ALREADY_STARTED")
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Timber.e("LE Advertise failed: ADVERTISE_FAILED_TOO_MANY_ADVERTISERS")
                ADVERTISE_FAILED_INTERNAL_ERROR -> Timber.e("LE Advertise failed: ADVERTISE_FAILED_INTERNAL_ERROR")
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> Timber.e("LE Advertise failed: ADVERTISE_FAILED_FEATURE_UNSUPPORTED")
                else -> Timber.e("LE Advertise failed: Unknown")
            }
        }
    }


    var bluetoothGattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(
            device: BluetoothDevice?,
            status: Int,
            newState: Int
        ) {
            super.onConnectionStateChange(device, status, newState)
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            //mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, YOUR_RESPONSE);
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(
                device,
                requestId,
                descriptor,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
        }
    }

    // All BLE characteristic UUIDs are of the form:
    // 0000XXXX-0000-1000-8000-00805f9b34fb
    // The assigned number for the Heart Rate Measurement characteristic UUID is
    // listed as 0x2A37, which is how the developer of the sample code could arrive at:
    // 00002a37-0000-1000-8000-00805f9b34fb
    companion object {
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2

        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        const val UUID_THROUGHPUT_MEASUREMENT = "0483dadd-6c9d-6ca9-5d41-03ad4fff4abb"
        const val UUID_THROUGHPUT_MEASUREMENT_CHAR = "00001524-0000-1000-8000-00805f9b34fb"
    }

    fun isMultipleAdvertisementSupported(): Boolean {
        return mBluetoothAdapter.isMultipleAdvertisementSupported
    }

    private fun enableBle() {
        Timber.i("There is possibility to use bluetooth")
        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    }

    private fun disableBle() {
        // Don't forget to unregister the ACTION_FOUND receiver.
    }
}

