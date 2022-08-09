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

class Metrics {

    /** The number of packets sent. */
    inner class Data {
        /** Number of GATT writes received. */
        var writeCount: UInt = 0u

        /** Number of bytes received. */
        var writeLen: UInt = 0u

        /** Transfer speed in bits per second (kbps). */
        var writeRate: UInt = 0u

        /** error count if BER is enabled. **/
        var errorCount: Int = 0

        fun clean() {
            writeCount = 0u
            writeLen = 0u
            writeRate = 0u
            errorCount = 0
        }
    }

    var mBleTestType: BleTestType = BleTestType.BT_TEST_TYPE_UNKNOWN
    private var mData = Data()
    private var mDelta: Long = -1
    private var mStart: Long = -1

    fun dumpStats() {
        Timber.i("[local] sent $mData.write_len bytes ($mData.write_len_KB KB) in $mDelta ms at $mData.write_rate kbps")
    }

    fun updateMetric(writeLen: Int, writeCount: Int, berError: Int) {
        updateTimer()
        mData.writeCount += writeCount.toUInt()
        mData.writeLen += writeLen.toUInt()
        mData.errorCount += berError
        mData.writeRate = ((mData.writeLen * 8u).toLong() / mDelta).toUInt()
    }

    fun start() {
        startTimer()
        mData.clean()
    }

    fun reset() {
        start()
    }

    fun stop() {
        stopTimer()
    }

    private fun startTimer() {
        mStart = System.nanoTime()
    }

    private fun updateTimer() {
        mDelta = (System.nanoTime() - mStart)
    }

    private fun stopTimer() {
        updateTimer()
    }
}
