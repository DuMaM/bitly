package pl.nowak.bitly.ecg

import com.github.mikephil.charting.data.Entry

// import kotlin.collections.ArrayDeque
data class EcgChartData(
    var label: String,
    var id: Int,
    var size: Int
) {
    var lineDataRestricted: ArrayDeque<Entry> = ArrayDeque(size)
    var cnt: Long = 0

    fun getLastTimestamp(): Float {
        return if (lineDataRestricted.size > 0) {
            return lineDataRestricted.last().x
        } else {
            Float.NaN
        }
    }

    fun update(x: Float, y: Float) {
        // add to the end
        val entry = Entry(x, y)
        if (lineDataRestricted.size > size) {
            lineDataRestricted.removeFirst()
        }
        lineDataRestricted.add(entry)
        cnt++
    }
}
