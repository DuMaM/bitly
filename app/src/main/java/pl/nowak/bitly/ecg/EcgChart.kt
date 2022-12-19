package pl.nowak.bitly.ecg

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet


class EcgChart : LineChart {
    constructor(ctx: Context) : super(ctx) {
        // defaultAxisSettings()
        chartSettings()
    }

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
        // defaultAxisSettings()
        chartSettings()
    }

    constructor(ctx: Context, attrs: AttributeSet, defStyle: Int) : super(ctx, attrs, defStyle) {
        // defaultAxisSettings()
        chartSettings()
    }

    private val setIndex = 0
    private val holoBlue = Color.rgb(51, 181, 229)
    private val holeDarkBlue = Color.rgb(103, 134, 147)
    private var numberOfData: Int = 0

    fun chartSettings() {

        // incognito mode for char obj
        legend.isEnabled = false
        description.isEnabled = false
        setTouchEnabled(true)
        setPinchZoom(false)

        // turn off one axis
        axisRight.setDrawGridLines(false)
        axisRight.setDrawLabels(false)
        axisRight.isEnabled = false

        // turn on another
        var yAxis = axisLeft
        yAxis.setDrawLabels(true)
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        yAxis.isEnabled = true
        yAxis.setDrawGridLines(true)
        yAxis.setDrawGridLinesBehindData(false)
        yAxis.gridColor = holeDarkBlue
        yAxis.axisMinimum = -200f
        yAxis.axisMaximum = 200f
        yAxis.isGranularityEnabled = true
        yAxis.granularity = 0.00001f

        // mode for X axis
        xAxis.setDrawLabels(false)
        xAxis.labelRotationAngle = 0f
        xAxis.isEnabled = true
        xAxis.setCenterAxisLabels(true)
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.isGranularityEnabled = true
        xAxis.granularity = 0.00001f

        // set data settings
        var lineSet = LineDataSet(emptyList<Entry>().toMutableList(), "data")
        // draw only space without line and dots
        lineSet.setDrawValues(false)
        lineSet.setDrawCircleHole(false)
        lineSet.setDrawCircles(false)
        lineSet.setDrawFilled(false)
        lineSet.lineWidth = 0.5f
        lineSet.color = holoBlue

        data = LineData(lineSet)

        //yAxis.axisMinimum = -5000f
        //yAxis.axisMaximum = 5000f
//        axisLeft.setDrawAxisLine(false)

    }

    var X_COUNT_MAX = 12000
    fun cleanUpOldEntries() {
        var set = data.dataSets[setIndex]
        var cnt = 0

        if (set.entryCount >= X_COUNT_MAX) {
            while (set.entryCount >= X_COUNT_MAX) {
                set.removeFirst()
                cnt++
            }
            for (i in 0..set.entryCount - 1) {
                var entryToChange = set.getEntryForIndex(i)
                entryToChange.x = entryToChange.x - cnt
            }
        }
    }


    private fun moveVisibleWindow(min: Float, max: Float) {
        val x: XAxis = xAxis
        if (min > 0) {
            x.axisMinimum = min
        }
        x.axisMaximum = max
    }

    fun addEntries(inData: List<Entry>?) {
        if (inData == null) {
            data.dataSets[setIndex].clear()
        } else {
            if (inData.isEmpty()) {
                return
            }


            inData.forEach {
                data.addEntry(it, setIndex)
            }
            moveVisibleWindow(data.xMax - 3f, data.xMax)

            //  cleanUpOldEntries()
//        if (inData.size > 1) {
//            xAxis.axisMaximum = inData.last().x
//            xAxis.axisMinimum = inData.first().x
//        }
        }

        // notify data has been updates
        data.notifyDataChanged()

        // let the chart know it's data has changed
        notifyDataSetChanged()

        // limit the number of visible entries
        // ecgChart.setVisibleXRangeMaximum(120f)
        // ecgChart.setVisibleYRange(30, AxisDependency.LEFT);

        // move to the latest entry
        //moveViewToX(data.xMax - 500f)

        // this automatically refreshes the chart (calls invalidate())
        // chart.moveViewTo(data.getXValCount()-7, 55f,
        // AxisDependency.LEFT);
        invalidate()
    }
}
