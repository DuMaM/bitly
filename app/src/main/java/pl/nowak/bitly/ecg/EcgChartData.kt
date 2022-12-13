package pl.nowak.bitly.ecg

import com.github.mikephil.charting.data.Entry

// import kotlin.collections.ArrayDeque
data class EcgChartData(
    val label: String,
    val id: Int,
    val size: Int
) {
    var lineDataRestricted: ArrayDeque<Entry> = ArrayDeque(size)

    @Volatile
    var newVal: Boolean = false

    fun clean() {
        lineDataRestricted.clear()
    }

    fun getLastTimestamp(): Float {
        return if (lineDataRestricted.size > 0) {
            return lineDataRestricted.last().x
        } else {
            Float.NaN
        }
    }

    fun update(x: Float, y: Float) {
        // add to the end
        val entry = Entry(x, y / 1000000)
        if (lineDataRestricted.size > size) {
            lineDataRestricted.removeFirst()
        }
        lineDataRestricted.add(entry)
        newVal = true
    }
}
