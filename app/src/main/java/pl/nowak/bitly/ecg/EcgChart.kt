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
        yAxis.setAxisMaxValue(200f)
        yAxis.setAxisMinValue(-200f)

        // mode for X axis
        xAxis.setDrawLabels(false)
        xAxis.labelRotationAngle = 0f
        xAxis.isEnabled = true
        xAxis.setCenterAxisLabels(true)
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(true)
        xAxis.position = XAxis.XAxisPosition.TOP_INSIDE

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

    fun addEntries(inData: ArrayDeque<Entry>) {
        inData.forEach {
            data.addEntry(it, setIndex)
        }

        if (inData.size > 1) {
            xAxis.axisMaximum = inData.last().x
            xAxis.axisMinimum = inData.first().x
        }

        // notify data has been updates
        data.notifyDataChanged()

        // let the chart know it's data has changed
        notifyDataSetChanged()

        // limit the number of visible entries
        // ecgChart.setVisibleXRangeMaximum(120f)
        // ecgChart.setVisibleYRange(30, AxisDependency.LEFT);

        // move to the latest entry
        //moveViewToX(item.getLastTimestamp())

        // this automatically refreshes the chart (calls invalidate())
        // chart.moveViewTo(data.getXValCount()-7, 55f,
        // AxisDependency.LEFT);
        invalidate()

    }

    private var numberOfData: Int = 0
    private val holoBlue = Color.rgb(51, 181, 229)
    private val holeDarkBlue = Color.rgb(103, 134, 147)

//    fun defaultDataSettings(vl: LineDataSet): LineDataSet {
//        // draw only space without line and dots
//        vl.setDrawFilled(true)
//        vl.setDrawValues(false)
//        vl.setDrawCircleHole(false)
//        vl.setDrawCircles(false)
//        vl.lineWidth = 0f
//
//        vl.fillColor = holoBlue
//        vl.fillAlpha = holoBlue
//        vl.color = holoBlue
//        return vl
//    }
//
//    fun defaultAxisSettings() {
//
//        axisLeft.axisMinimum = -1f
//        axisLeft.axisMaximum = 10f
//
//        animateX(1800, Easing.EaseInExpo)
//    }
}
