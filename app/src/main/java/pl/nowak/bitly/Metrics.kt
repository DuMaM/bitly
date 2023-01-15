package pl.nowak.bitly

import timber.log.Timber
import kotlin.math.sqrt


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

    private var m_n = 0
    private var m_oldM = 0f
    private var m_newM = 0f
    private var m_newS = 0f
    private var m_oldS = 0f

    private fun addVariable(x: Float) {
        m_n++

        // See Knuth TAOCP vol 2, 3rd edition, page 232
        if (m_n == 1) {
            m_oldM = x
            m_newM = x
            m_oldS = 0.0f
        } else {
            m_newM = m_oldM + (x - m_oldM) / m_n
            m_newS = m_oldS + (x - m_oldM) * (x - m_newM)

            // set up for next iteration
            m_oldM = m_newM
            m_oldS = m_newS
        }
    }

    fun getMean(): Float {
        return if (m_n > 0) m_newM else 0.0f
    }

    fun getVariance(): Float {
        return if (m_n > 1) m_newS / (m_n - 1) else 0.0f
    }

    fun getStandardDeviation(): Float {
        return sqrt(getVariance())
    }

    fun clean() {
        m_n = 0
        m_oldM = 0f
        m_newM = 0f
        m_newS = 0f
        m_oldS = 0f

        mData.clean()
    }

    fun getStats(): String {
        return listOf<String>(
            "Time Mean: ${String.format("%.6f", getMean())}",
            "Time Sig: ${String.format("%.6f", getStandardDeviation())}",
            "Data Size: ${mData.writeLen} bytes",
            "Throughput ${mData.writeRate / 1024u} kbps"
        ).joinToString("\n")
    }

    @Suppress("unused")
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
        clean()
        Timber.v("Connection metrics measurement started")
    }

    fun reset() {
        start()
    }

    @Suppress("unused")
    fun stop() {
        stopTimer()
    }

    private fun startTimer() {
        mStart = System.nanoTime().toULong()
        mDelta = 0u
    }

    private fun updateTimer() {
        val tmpDelta: ULong = ((System.nanoTime().toULong()) - mStart) / 1000u
        addVariable((tmpDelta - mDelta).toFloat())
        mDelta = tmpDelta
    }

    private fun stopTimer() {
        updateTimer()
    }
}
