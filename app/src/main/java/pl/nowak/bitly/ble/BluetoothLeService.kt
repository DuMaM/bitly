package pl.nowak.bitly.ble

import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresPermission
import pl.nowak.bitly.BleTestType
import pl.nowak.bitly.Metrics
import timber.log.Timber
import java.util.*
import kotlin.experimental.and
import kotlin.reflect.KFunction1


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class BluetoothLeService : Service() {

    private var onConnectionStatusChange: ((String) -> Unit)? = null
    private val mBluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val mBluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val mBluetoothAdvertiser: BluetoothLeAdvertiser by lazy {
        mBluetoothAdapter.bluetoothLeAdvertiser
    }

    private lateinit var mGattServer: BluetoothGattServer

    private var mBluetoothGattService: BluetoothGattService = BluetoothGattService(
        UUID.fromString(UUID_THROUGHPUT_MEASUREMENT), BluetoothGattService.SERVICE_TYPE_PRIMARY
    )

    private val mBluetoothDevices: HashSet<BluetoothDevice> = HashSet()


    private val mBleDataResponseResponse: AdvertiseData = AdvertiseData.Builder().setIncludeDeviceName(true).build()

    private var currentAdvertisingSet: AdvertisingSet? = null
    private val mBluetoothAdvParameters: AdvertiseSettings =
        AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED).setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(true).build()
    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    private val mMetrics: Metrics = Metrics()

    @RequiresPermission(BLUETOOTH_CONNECT)
    override fun onBind(intent: Intent): IBinder {
        if (!isEnabled()) {
            mBluetoothAdapter.enable()
            Timber.i("Enabling bluetooth")
        } else {
            Timber.i("Bluetooth is working")
        }

        val characteristic = BluetoothGattCharacteristic(
            UUID.fromString(UUID_THROUGHPUT_MEASUREMENT_CHAR),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        characteristic.addDescriptor(
            BluetoothGattDescriptor(
                UUID.fromString("00001525-0000-1000-8000-00805f9b34fb"),
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
        )

        mBluetoothGattService.addCharacteristic(characteristic)
        mGattServer = mBluetoothManager.openGattServer(this, bluetoothGattServerCallback)
        mGattServer.addService(mBluetoothGattService)

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
    fun initialize(connectionChangeCallback: KFunction1<String, Unit>) {
        onConnectionStatusChange = connectionChangeCallback
        mBluetoothDevices.clear()
        if (!isEnabled()) {
            enableBle()
        }
    }

    private fun isEnabled(): Boolean {
        if (mBluetoothAdapter.isEnabled) {
            Timber.i("Bluetooth is enabled")
        } else {
            Timber.i("Bluetooth is not enabled")
        }
        return mBluetoothAdapter.isEnabled
    }

    @RequiresPermission(allOf = [BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE])
    fun startAdv(): Boolean {
        if (isMultipleAdvertisementSupported()) {
            Timber.i("Multiple advertisement supported")
        } else {
            Timber.i("Multiple advertisement not supported")
            return false
        }

        // After onAdvertisingSetStarted callback is called, you can modify the
        // advertising data and scan response data:
        val pUuid = ParcelUuid(UUID.fromString(UUID_THROUGHPUT_MEASUREMENT))
        val mBleData = AdvertiseData.Builder().setIncludeDeviceName(false).addServiceUuid(pUuid).build()

        val isNameChanged = BluetoothAdapter.getDefaultAdapter().setName("Nordic_Performance_Test")
        if (isNameChanged) {
            Timber.i("Name changed")
        } else {
            Timber.i("Name not changed")
        }

        // nonblocking
        // status via callback
        mBluetoothAdvertiser.startAdvertising(mBluetoothAdvParameters, mBleData, mBleDataResponseResponse, mAdvCallback)
        Handler(Looper.getMainLooper()).postDelayed({ stopAdv() }, 10000)

        return true
    }

    @RequiresPermission(allOf = [BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE])
    private fun stopAdv() {
        // Can also stop and restart the advertising
        currentAdvertisingSet?.enableAdvertising(false, 0, 0)
        // Wait for onAdvertisingEnabled callback...
        currentAdvertisingSet?.enableAdvertising(true, 60000, 200)
        // Wait for onAdvertisingEnabled callback...

        // Wait for onScanResponseDataSet callback...
        mBluetoothAdvertiser.stopAdvertising(mAdvCallback)
    }

    fun numberToByteArray(data: Number, size: Int = 4): ByteArray = ByteArray(size) { i -> (data.toLong() shr (i * 8)).toByte() }

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

    private var bluetoothGattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            Timber.w("ATT MTU changed to $mtu")
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices.add(device)
                    val msg = "Connected to device: " + device.address
                    Timber.v(msg)
                    onConnectionStatusChange?.invoke(msg)
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices.remove(device)
                    val msg = "Disconnected from device: " + device.address
                    Timber.v(msg)
                    onConnectionStatusChange?.invoke(msg)
                }
            } else {
                mBluetoothDevices.remove(device)
                Timber.e("Error when connecting: $status")
            }
        }

        @RequiresPermission(allOf = [BLUETOOTH_CONNECT])
        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Timber.d("Device tried to read characteristic: " + characteristic.uuid)
            Timber.d("Value: " + Arrays.toString(characteristic.value))
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,  /* value (optional) */null)
                return
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
            Timber.v("Notification sent. Status: $status")
        }

        @RequiresPermission(allOf = [BLUETOOTH_CONNECT])
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            Timber.v("Characteristic Write request: " + Arrays.toString(value))
            var status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
            if (characteristic?.uuid == UUID.fromString(UUID_THROUGHPUT_MEASUREMENT)) {

                when (mMetrics.mBleTestType) {
                    BleTestType.BT_TEST_TYPE_ANALOG -> {
                        Timber.i("Received analog test data")
                    }
                    BleTestType.BT_TEST_TYPE_BER -> {
                        Timber.i("Received BER test data")
                    }
                    BleTestType.BT_TEST_TYPE_SIM -> {
                        Timber.i("Received simulator test data")
                    }
                    BleTestType.BT_TEST_TYPE_SIMPLE -> {
                        Timber.i("Received simple test data")
                    }
                    else -> {
                        Timber.i("Received BER test data")
                    }
                }

                status = BluetoothGatt.GATT_SUCCESS
            }

            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status,  /* No need to respond with an offset */0,  /* No need to respond with a value */null)
            }
        }

        fun notificationsDisabled(characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid !== UUID.fromString(UUID_THROUGHPUT_MEASUREMENT)) {
                return
            }
            mMetrics.stop()
        }


        fun notificationsEnabled(characteristic: BluetoothGattCharacteristic, indicate: Boolean) {
            if (characteristic.uuid !== UUID.fromString(UUID_THROUGHPUT_MEASUREMENT)) {
                return
            }
            if (!indicate) {
                return
            }
        }

        @RequiresPermission(allOf = [BLUETOOTH_CONNECT])
        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            Timber.d("Device tried to read descriptor: " + descriptor.uuid)
            Timber.d("Value: " + Arrays.toString(descriptor.value))
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,  /* value (optional) */null)
                return
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.value)
        }

        @RequiresPermission(allOf = [BLUETOOTH_CONNECT])
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            Timber.v("Descriptor Write Request " + descriptor.uuid + " " + value.contentToString())
            var status = BluetoothGatt.GATT_SUCCESS
            if (descriptor.uuid == UUID.fromString(UUID_THROUGHPUT_MEASUREMENT_DES)) {
                if (value.size != 1) {
                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
                } else {
                    status = BluetoothGatt.GATT_SUCCESS
                    descriptor.value = value

                    var testType: Int = BleTestType.BT_TEST_TYPE_UNKNOWN.ordinal
                    for (b in value) {
                        testType = (testType shl 8) + (b and 0xFF.toByte())
                    }

                    when (testType) {
                        BleTestType.BT_TEST_TYPE_RESET.ordinal -> {
                            mMetrics.reset()
                            mMetrics.mBleTestType = BleTestType.BT_TEST_TYPE_UNKNOWN
                        }
                        BleTestType.BT_TEST_TYPE_ANALOG.ordinal -> {
                            mMetrics.mBleTestType = BleTestType.BT_TEST_TYPE_ANALOG
                        }
                        BleTestType.BT_TEST_TYPE_BER.ordinal -> {
                            mMetrics.mBleTestType = BleTestType.BT_TEST_TYPE_BER
                        }
                        BleTestType.BT_TEST_TYPE_SIM.ordinal -> {
                            mMetrics.mBleTestType = BleTestType.BT_TEST_TYPE_SIM
                        }
                        BleTestType.BT_TEST_TYPE_SIMPLE.ordinal -> {
                            mMetrics.mBleTestType = BleTestType.BT_TEST_TYPE_SIMPLE
                        }
                        else -> {
                            mMetrics.mBleTestType = BleTestType.BT_TEST_TYPE_UNKNOWN
                        }
                    }
                }
            } else {
                status = BluetoothGatt.GATT_SUCCESS
                descriptor.value = value
            }
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status,  /* No need to respond with offset */0,  /* No need to respond with a value */null)
            }
        }
    }

    // All BLE characteristic UUIDs are of the form:
    // 00001234-0000-1000-8000-00805f9b34fb
    // The assigned number for the Heart Rate Measurement characteristic UUID is
    // listed as 0x2A37, which is how the developer of the sample code could arrive at:
    // 00002a37-0000-1000-8000-00805f9b34fb
    companion object {
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2

        const val UUID_THROUGHPUT_MEASUREMENT = "0483dadd-6c9d-6ca9-5d41-03ad4fff4abb"
        const val UUID_THROUGHPUT_MEASUREMENT_CHAR = "00001524-0000-1000-8000-00805f9b34fb"
        const val UUID_THROUGHPUT_MEASUREMENT_DES = "00001525-0000-1000-8000-00805f9b34fb"
    }

    private fun isMultipleAdvertisementSupported(): Boolean {
        return mBluetoothAdapter.isMultipleAdvertisementSupported
    }

    private fun enableBle() {
        Timber.i("There is possibility to use bluetooth")
        // Register for broadcasts when a device is discovered.
    }

    private fun disableBle() {
        // Don't forget to unregister the ACTION_FOUND receiver.
    }

    private fun ensureBleFeaturesAvailable() {
        if (!mBluetoothAdapter.isEnabled) {
            // Make sure bluetooth is enabled.
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    @RequiresPermission(allOf = [BLUETOOTH_CONNECT])
    fun disconnectFromDevices() {
        Timber.d("Disconnecting devices...")
        for (device in mBluetoothDevices) {
            Timber.d("Devices: " + device.address + " " + device.name)
            mGattServer.cancelConnection(device)
            Handler(Looper.getMainLooper()).postDelayed({
                if (mBluetoothManager.getConnectionState(device, BluetoothGatt.GATT) == BluetoothGatt.STATE_CONNECTED) {
                    Timber.i("Device disconnected: " + device.address)
                } else {
                    Timber.i("Device unable to drop connection: " + device.address)
                }
            }, 1000)
        }
    }
}
