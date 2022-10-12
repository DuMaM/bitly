package pl.nowak.bitly.ecg

import com.github.mikephil.charting.data.Entry

data class EcgChartData(
    var label: String,
    var id: Int,
    var size: Int
) {
    private var _lineDataAll = ArrayDeque<Entry>(size)
    var lineDataRestricted = ArrayDeque<Entry>(size)

    fun getLastTimestamp(): Float {
        return if (lineDataRestricted.size > 0) {
            return lineDataRestricted.last().x
        } else {
            Float.NaN
        }
    }

    fun update(entry: Entry) {
        // add to the end
        _lineDataAll.add(entry)

        lineDataRestricted.removeFirst()
        lineDataRestricted.add(entry)
    }
}
