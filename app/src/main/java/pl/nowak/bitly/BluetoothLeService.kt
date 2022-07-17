package pl.nowak.bitly

import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
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

    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null

    //private val mBleDataResponseResponse: AdvertiseData = null
    //    private val mBleDataResponseResponse: AdvertiseData = AdvertiseData.Builder()
    //       .setIncludeDeviceName(true)
    //       .setIncludeTxPowerLevel(true).addServiceData(0x09)
    //       .build()
    private var mConnectionState = STATE_DISCONNECTED
    var devicesMap: HashMap<String, BluetoothDevice> = hashMapOf()

    private var currentAdvertisingSet: AdvertisingSet? = null
    private val mBluetoothAdvParameters: AdvertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
        .setConnectable(true)
        .build()

//    private val mBluetoothAdvParameters = AdvertisingSetParameters.Builder()
//        .setLegacyMode(false) // True by default, but set here as a reminder.
//        .setConnectable(true)
//        .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
//        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
//        .build()

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                mConnectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                Timber.i("Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Timber.i(
                    "Attempting to start service discovery:" +
                            mBluetoothGatt!!.discoverServices()
                )
            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                mConnectionState = STATE_DISCONNECTED
                Timber.i("Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            }
            Timber.w("onServicesDiscovered received: $status")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Timber.i("onCharacteristicChanged received: $characteristic")
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic
    ) {
        val intent = Intent(action)

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        // For all other profiles, writes the data formatted in HEX.
        val data = characteristic.value
        if (data != null && data.size > 0) {
            val stringBuilder = StringBuilder(data.size)
            for (byteChar in data) stringBuilder.append(String.format("%02X ", byteChar))
            intent.putExtra(
                EXTRA_DATA, """
                                 ${String(data)}
                                 $stringBuilder
                                 """.trimIndent()
            )
        }

        sendBroadcast(intent)
    }

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

        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    private val mBinder: IBinder = LocalBinder()

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

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun connect(address: String?): Boolean {
        if (address == null) {
            Timber.w("Unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            Timber.d("Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }
        val device = mBluetoothAdapter.getRemoteDevice(address)
        if (device == null) {
            Timber.w("Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Timber.d("Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        if (mBluetoothGatt == null) {
            Timber.w("BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (mBluetoothGatt == null) {
            Timber.w("BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.readCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        if (mBluetoothGatt == null) {
            Timber.w("BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
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
        val pUuid = ParcelUuid(UUID_THROUGHPUT_MEASUREMENT)
        val mBleData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(pUuid)
            .build()

        mBluetoothAdvertiser.startAdvertising(mBluetoothAdvParameters, mBleData, mAdvCallback)
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


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return if (mBluetoothGatt == null) null else mBluetoothGatt!!.services
    }

    companion object {
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2

        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        val UUID_THROUGHPUT_MEASUREMENT = UUID.fromString("0483dadd-6c9d-6ca9-5d41-03ad4fff4abb")
    }

    fun isMultipleAdvertisementSupported(): Boolean {
        return mBluetoothAdapter.isMultipleAdvertisementSupported
    }

    private fun enableBle() {
        Timber.i("There is possibility to use bluetooth")
        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    private fun disableBle() {
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }


    private var scanning = false

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
        }
    }

    fun scanLeDevice() {
        if (!scanning) {
            startLeScan()
        } else {
            stopLeScan()
        }
    }

    fun startLeScan() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            Handler(Looper.getMainLooper()).postDelayed({
                stopLeScan()
            }, SCAN_PERIOD)
            scanning = true
            mBluetoothLEScanner.startScan(leScanCallback)
        }
    }

    fun stopLeScan() {
        if (scanning) {
            scanning = false
            mBluetoothLEScanner.stopScan(leScanCallback)
        }
    }

    // add async pulling of discovery requests
// https://developer.android.com/guide/topics/connectivity/bluetooth/find-bluetooth-devices
// Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

                    devicesMap[device.address] = device
                }
            }
        }
    }
}
