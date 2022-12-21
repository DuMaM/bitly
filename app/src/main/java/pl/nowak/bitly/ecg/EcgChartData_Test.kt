package pl.nowak.bitly.ecg

import CircularArray
import android.graphics.Canvas
import com.androidplot.Plot
import com.androidplot.PlotListener
import com.androidplot.SeriesRegistry
import com.androidplot.ui.Formatter
import com.androidplot.ui.SeriesBundle
import com.androidplot.ui.SeriesRenderer
import com.androidplot.xy.OrderedXYSeries
import com.androidplot.xy.XYSeries
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

data class EcgChartData_Test(
    val label: String,
    val id: Int,
    val size: Int
) : XYSeries, PlotListener, OrderedXYSeries {

    class SignalStats {
        var n = 0
        var K = 0f
        var Ex = 0f
        var Ex2 = 0f

        fun add_variable(x: Float) {
            if (n == 0)
                K = x
            n += 1
            Ex += x - K
            Ex2 += (x - K) * (x - K)
        }

        fun remove_variable(x: Float) {
            n -= 1
            Ex -= x - K
            Ex2 -= (x - K) * (x - K)
        }

        fun get_mean(): Float {
            return K + Ex / n
        }

        fun get_variance(): Float {
            return (Ex2 - Ex * Ex / n) / (n - 1)
        }

        fun clean() {
            K = 0f
            Ex = 0f
            Ex2 = 0f
            n = 0
        }
    }

    data class Entry(val x: Float, val y: Float)

    var stats: SignalStats = SignalStats()

    private var lineDataRestricted: CircularArray<Entry> = CircularArray(size)
    private var seriesCounter = 0
    private val seriesResolutionCounter = 16777215
    private var cnt = 0
    val mutex = Mutex()

    @Volatile
    var newVal: Boolean = false

    fun clean() {
        runBlocking {
            mutex.withLock {
                lineDataRestricted.clean()
                seriesCounter = 0
                stats.clean()
                cnt = 0
                Timber.i("Lead: $label was cleared")
            }
        }
    }

    suspend fun update(x: Float, y: Float) {
        mutex.withLock {

//            if (cnt % 4 != 0) {
//                return
//            }

            var x_sec = (x + seriesCounter * seriesResolutionCounter) / 1000000f
            val y_scaled = y / (1000000f)
            if (lineDataRestricted.size > 0 && x_sec < lineDataRestricted.last().x) {
                seriesCounter++
                x_sec = (x + seriesCounter * seriesResolutionCounter) / 1000000f
            }
            // add to the end
            // time in ms, value in mv
            val entry = Entry(x_sec, y_scaled)
//            if (lineDataRestricted.size > size) {
//                val removed = lineDataRestricted.removeFirst()
//                stats.remove_variable(removed.y)
//            }
            lineDataRestricted.add(entry)
            newVal = true
            cnt++
            stats.add_variable(y_scaled)

            // Timber.i("For: ${label}:Variance is ${stats.get_mean() * 100000} ")
        }
    }


    override fun getTitle(): String {
        return label
    }

    override fun size(): Int {
        return lineDataRestricted.size
    }

    override fun getX(p0: Int): Number {
        return lineDataRestricted[p0].x
    }

    override fun getY(p0: Int): Number {
        return lineDataRestricted[p0].y
    }

//    override fun minMax(): RectRegion {
//        if (lineDataRestricted.size == 0)
//            return RectRegion()
//
//        val min = lineDataRestricted[lineDataRestricted.tail]
//        val minCords = XYCoords(min.x, min.y)
//        val max = lineDataRestricted[lineDataRestricted.head]
//        val maxCords = XYCoords(min.x, min.y)
//
//        return RectRegion(minCords, maxCords)
//    }

    override fun getXOrder(): OrderedXYSeries.XOrder {
        return com.androidplot.xy.OrderedXYSeries.XOrder.ASCENDING
    }

    override fun onBeforeDraw(
        p0: Plot<*, out Formatter<*>, out SeriesRenderer<*, *, *>, out SeriesBundle<*, out Formatter<*>>, out SeriesRegistry<out SeriesBundle<*, out Formatter<*>>, *, out Formatter<*>>>?,
        p1: Canvas?
    ) {
        runBlocking {
            mutex.lock()
        }
    }

    override fun onAfterDraw(
        p0: Plot<*, out Formatter<*>, out SeriesRenderer<*, *, *>, out SeriesBundle<*, out Formatter<*>>, out SeriesRegistry<out SeriesBundle<*, out Formatter<*>>, *, out Formatter<*>>>?,
        p1: Canvas?
    ) {
        runBlocking {
            mutex.unlock()
        }
    }
}
