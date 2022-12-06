package pl.nowak.bitly.ble

import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pl.nowak.bitly.BleTestType
import pl.nowak.bitly.Metrics
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class BluetoothLeService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO) // the scope of MyUIClass, uses Dispatchers.Main

    private var onConnectionStatusChange: ((String) -> Unit)? = null

    private val mBluetoothManager: BluetoothManager by lazy {
        //getSystemService(BluetoothManager::class.java) as BluetoothManager
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val mBluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter

        //mBluetoothManager.adapter
    }

    private val mBluetoothAdvertiser: BluetoothLeAdvertiser by lazy {
        mBluetoothAdapter.bluetoothLeAdvertiser
    }

    private lateinit var mGattServer: BluetoothGattServer

    private val mBluetoothGattService: BluetoothGattService by lazy {
        val service = BluetoothGattService(UUID.fromString(UUID_THROUGHPUT_MEASUREMENT), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            UUID.fromString(UUID_THROUGHPUT_MEASUREMENT_CHAR),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        characteristic.addDescriptor(
            BluetoothGattDescriptor(
                UUID.fromString(UUID_THROUGHPUT_MEASUREMENT_DES),
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
        )

        service.addCharacteristic(characteristic)

        // return ready object
        service
    }

    private val mBluetoothDevices: HashSet<BluetoothDevice> = HashSet()

    private val mBleDataResponseResponse: AdvertiseData = AdvertiseData.Builder().setIncludeDeviceName(true).build()

    private val mBluetoothAdvParameters: AdvertiseSettings =
        AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED).setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(true).build()

    private var mMetrics: Metrics = Metrics()

    /**
     * Service methods
     */
    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent): IBinder {
        mBluetoothDevices.clear()

        Timber.i("service connected with main thread")
        return mBinder
    }

    @RequiresPermission(allOf = [BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE])
    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        Timber.i("service disconnected from main thread")
        //disconnectFromDevices()
        return super.onUnbind(intent)
    }

    /**
     * Scanning
     */
    private fun isMultipleAdvertisementSupported(): Boolean {
        return mBluetoothAdapter.isMultipleAdvertisementSupported
    }

    @RequiresPermission(allOf = [BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE])
    fun startAdv(): Boolean {


        if (isMultipleAdvertisementSupported()) {
            Timber.i("multiple advertisement supported")
        } else {
            Timber.i("multiple advertisement not supported")
            return false
        }

        val isNameChanged = BluetoothAdapter.getDefaultAdapter().setName("Nordic_Performance_Test")
        if (isNameChanged) {
            Timber.i("device ble name was changed")
        }

        // After onAdvertisingSetStarted callback is called, you can modify the
        // advertising data and scan response data:
        val pUuid = ParcelUuid(UUID.fromString(UUID_THROUGHPUT_MEASUREMENT))
        val mBleData = AdvertiseData.Builder().setIncludeDeviceName(false).addServiceUuid(pUuid).build()

        // nonblocking
        // status via callback
        mBluetoothAdvertiser.startAdvertising(mBluetoothAdvParameters, mBleData, mBleDataResponseResponse, mAdvCallback)
        scope.async {
            delay(5000L)
            stopAdv()
        }

        Timber.i("advertisement started")

        return true
    }

    @RequiresPermission(allOf = [BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE])
    private suspend fun stopAdv() {
        // Wait for onScanResponseDataSet callback...
        mBluetoothAdvertiser.stopAdvertising(mAdvCallback)
        Timber.i("advertisement stopped")
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

//    suspend fun sendResponse(device: BluetoothDevice?, requestId: Int, status: Int, offset: Int, value: ByteArray) {
//        withContext(Dispatchers.IO) {
//            val response = async { mGattServer.sendResponse(device, requestId, status, offset, value) }
//            response.await()
//        }
//    }
// ... somewhere outside BluetoothGattCallback

    /**
     * Server
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    @RequiresPermission(allOf = [BLUETOOTH_CONNECT])
    fun mBluetoothServerFlow(): Flow<UIntArray> = callbackFlow {

        var byteBuffer: ByteArray = ByteArray(4)
        var dataBuffer = UIntArray(14)
        var pos = 0
        var ecgPackPos = 0

        fun setCommunication() {
            ecgPackPos = 0
            pos = 0
        }

        fun ByteArray.getUIntAt(idx: Int, size: Int = 4, isBigEndian: Boolean = true): UInt {
            return if (isBigEndian) {
                ((this[idx].toUInt() and 0xFFu) shl 24) or
                        ((this[idx + 1].toUInt() and 0xFFu) shl 16) or
                        ((this[idx + 2].toUInt() and 0xFFu) shl 8) or
                        (this[idx + 3].toUInt() and 0xFFu)
            } else {
                ((this[idx + 3].toUInt() and 0xFFu) shl 24) or
                        ((this[idx + 2].toUInt() and 0xFFu) shl 16) or
                        ((this[idx + 1].toUInt() and 0xFFu) shl 8) or
                        (this[idx].toUInt() and 0xFFu)
            }
        }


        fun ByteArray.getIntAt(index: Int = 0, size: Int = 4, isBigEndian: Boolean = true): Int {
            if (size > Int.SIZE_BYTES) {
                return 0
            }

            var slice = this
            if (slice.size > size) {
                slice = slice.sliceArray(IntRange(index, index + size - 1))
            }
            return ByteBuffer.wrap(slice).int
        }

        fun UInt.toByteArray(isBigEndian: Boolean = true): ByteArray {
            // https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/#kotlin.ByteArray
            var bytes = byteArrayOf()

            var n = this

            while ((UInt.SIZE_BYTES - bytes.count()) > 0) {
                if (n == 0x00u) {
                    bytes += n.toByte()
                } else {
                    while (n != 0x00u) {
                        val b = n.toByte()

                        bytes += b

                        n = n.shr(Byte.SIZE_BITS)
                    }
                }
            }

            return if (isBigEndian) {
                bytes.reversedArray()
            } else {
                bytes
            }
        }

        val callback = object : BluetoothGattServerCallback() { // Implementation of some callback interface
            override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                Timber.w("ATT MTU changed to $mtu")
            }

            @RequiresPermission(allOf = [BLUETOOTH_CONNECT])
            override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

                if (!this@BluetoothLeService::mGattServer.isInitialized) {
                    Timber.e("gatt is not initiated but executed")
                    return
                }

                Timber.d("Device tried to read characteristic: " + characteristic.uuid)
                val metricsData = mMetrics.mData

                if (characteristic.uuid.toString() == UUID_THROUGHPUT_MEASUREMENT_CHAR) {
                    Timber.d("Character request got it ${metricsData.writeRate}")
                    val response =
                        metricsData.writeCount.toByteArray(false) +
                                metricsData.writeLen.toByteArray(false) +
                                metricsData.writeRate.toByteArray(false) +
                                metricsData.errorCount.toByteArray(false)
                    Timber.v("Sending " + response.contentToString())
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response)
                } else {
                    Timber.e("Undefined response for characteristic request")
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,  /* value (optional) */null)
                }
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
                // todo check if characteristic already have set this value

                if (!this@BluetoothLeService::mGattServer.isInitialized) {
                    Timber.e("gatt is not initiated but executed")
                    return
                }

                var status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                if (characteristic == null) {
                    Timber.w("Characteristic are missing")
                    mGattServer.sendResponse(device, requestId, status, 0, null)
                    return
                }

                if (value == null) {
                    Timber.w("Value was empty")
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                    return
                }

                if (characteristic.uuid.toString() == UUID_THROUGHPUT_MEASUREMENT_CHAR) {
                    mMetrics.updateMetric(value.size.toUInt(), 0u)

                    value.forEach { element ->
                        byteBuffer[pos] = element
                        pos++

                        if (pos == byteBuffer.size - 1) {
                            // clear last value to fill up buffer
                            byteBuffer[pos] = 0

                            // save data
                            dataBuffer[ecgPackPos] = byteBuffer.getUIntAt(0, byteBuffer.size, false)

                            // clean up after operation
                            pos = 0

                            // mark new data in buffer
                            ecgPackPos++
                        }

                        if (ecgPackPos == dataBuffer.size) {
                            //Timber.v(dataBuffer.contentToString())
                            ecgPackPos = 0

                            trySendBlocking(dataBuffer)
                        }
                    }

                } else {
                    Timber.v("Characteristic Write Request " + characteristic.uuid + " " + value.contentToString())
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                    Timber.e("Not supported request")
                }

                if (responseNeeded) {
                    mGattServer.sendResponse(device, requestId, status, 0, null)
                }
            }

            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                super.onConnectionStateChange(device, status, newState)
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    mBluetoothDevices.remove(device)
                    Timber.e("Error when connecting: $status")
                    return
                }

                var msg = when (newState) {
                    BluetoothGatt.STATE_CONNECTED -> {
                        mBluetoothDevices.add(device)
                        "Connected to device: " + device.address
                        // onConnectionStatusChange?.invoke(msg)
                    }
                    BluetoothGatt.STATE_DISCONNECTED -> {
                        mBluetoothDevices.remove(device)
                        "Disconnected from device: " + device.address
                        // onConnectionStatusChange?.invoke(msg)
                    }
                    else -> {
                        "Unhandled connections change"
                    }
                }
                Timber.v(msg)
            }

            @RequiresPermission(allOf = [BLUETOOTH_CONNECT])
            override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor)

                if (!this@BluetoothLeService::mGattServer.isInitialized) {
                    Timber.e("gatt is not initiated but executed")
                    return
                }

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
                var status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED

                if (!this@BluetoothLeService::mGattServer.isInitialized) {
                    Timber.e("gatt is not initiated but executed")
                    return
                }

                if (descriptor.uuid.toString() == UUID_THROUGHPUT_MEASUREMENT_DES) {
                    if (value.size != 1) {
                        status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
                    } else {
                        status = BluetoothGatt.GATT_SUCCESS
                        descriptor.value = value

                        var testType: Int = BleTestType.BT_TEST_TYPE_UNKNOWN.ordinal
                        for (b in value) {
                            testType = (testType shl 8) + (b and 0xFF.toByte())
                        }

                        setCommunication()

                        mMetrics.reset()
                        when (testType) {
                            BleTestType.BT_TEST_TYPE_RESET.ordinal -> {
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
                    descriptor.value = value
                }

                if (responseNeeded) {
                    mGattServer.sendResponse(device, requestId, status, 0, null)
                }
            }

//            override fun onNextValue(value: T) {
//                // To avoid blocking you can configure channel capacity using
//                // either buffer(Channel.CONFLATED) or buffer(Channel.UNLIMITED) to avoid overfill
//                trySendBlocking(value)
//                    .onFailure { throwable ->
//                        // Downstream has been cancelled or failed, can log here
//                    }
//            }


//            override fun onApiError(cause: Throwable) {
//                cancel(CancellationException("API Error", cause))
//            }
//
//            override fun onCompleted() = channel.close()
        }

        mGattServer = mBluetoothManager.openGattServer(application.applicationContext, callback)
        mGattServer.addService(mBluetoothGattService)

        /*
         * Suspends until either 'onCompleted'/'onApiError' from the callback is invoked
         * or flow collector is cancelled (e.g. by 'take(1)' or because a collector's coroutine was cancelled).
         * In both cases, callback will be properly unregistered.
         */
        awaitClose { mGattServer.clearServices() }
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

    @RequiresPermission(allOf = [BLUETOOTH_CONNECT])
    fun disconnectFromDevices() {
        Timber.d("Disconnecting devices...")
        for (device in mBluetoothDevices) {
            Timber.d("Devices: " + device.address + " " + device.name)
            if (this@BluetoothLeService::mGattServer.isInitialized) {
                mGattServer.cancelConnection(device)
            }
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
