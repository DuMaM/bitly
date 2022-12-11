package pl.nowak.bitly

import timber.log.Timber


enum class BleTestType {
    BT_TEST_TYPE_UNKNOWN,
    BT_TEST_TYPE_RESET,
    BT_TEST_TYPE_SIMPLE,
    BT_TEST_TYPE_BER,
    BT_TEST_TYPE_ANALOG,
    BT_TEST_TYPE_SIM
}

/** The number of packets sent. */
data class MetricsData(
    /** Number of GATT writes received. */
    var writeCount: UInt = 0u,

    /** Number of bytes received. */
    var writeLen: UInt = 0u,

    /** Transfer speed in bits per second (kbps). */
    var writeRate: UInt = 0u,

    /** error count if BER is enabled. **/
    var errorCount: UInt = 0u
) {
    fun clean() {
        writeCount = 0u
        writeLen = 0u
        writeRate = 0u
        errorCount = 0u
    }
}

// https://www.baeldung.com/kotlin/unsigned-integers
class Metrics {

    var mBleTestType: BleTestType = BleTestType.BT_TEST_TYPE_UNKNOWN
    var mData = MetricsData()

    private var mDelta: ULong = 0u
    private var mStart: ULong = 0u

    private fun dumpStats(writeLen: UInt = 0u) {
        Timber.i("[local] got $writeLen bytes (in total ${mData.writeLen}b | ${mData.writeLen / (8u * 1024u)} KB) in ${mDelta / 1000u} ms at ${mData.writeRate / 1024u} kbps")
    }

    fun updateMetric(writeLen: UInt, berError: UInt) {
        updateTimer()
        mData.writeCount++
        mData.writeLen += writeLen
        mData.errorCount += berError
        mData.writeRate = ((mData.writeLen.toULong() * 8u * 1000000u) / mDelta).toUInt()
        // dumpStats(writeLen)
    }

    fun start() {
        startTimer()
        mData.clean()
        Timber.v("Connection metrics measurement started")
    }

    fun reset() {
        start()
    }

    fun stop() {
        stopTimer()
    }

    private fun startTimer() {
        mStart = System.nanoTime().toULong() / 1000u
        mDelta = 0u
    }

    private fun updateTimer() {
        mDelta = (System.nanoTime().toULong() / 1000u) - mStart
    }

    private fun stopTimer() {
        updateTimer()
    }
}
