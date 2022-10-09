package pl.nowak.bitly

import com.github.mikephil.charting.data.LineDataSet

data class EcgChartData(
    var label: String,
    var id: Int
) {
    var chart: LineDataSet = LineDataSet(ArrayList(), "Data")
}
