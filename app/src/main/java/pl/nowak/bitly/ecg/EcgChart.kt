package pl.nowak.bitly.ecg

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.github.mikephil.charting.charts.LineChart


class EcgChart : LineChart {
    constructor(ctx: Context) : super(ctx) {
        // defaultAxisSettings()
        settins()
    }

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
        // defaultAxisSettings()
        settins()
    }

    constructor(ctx: Context, attrs: AttributeSet, defStyle: Int) : super(ctx, attrs, defStyle) {
        // defaultAxisSettings()
        settins()
    }

    fun settins() {
        isAutoScaleMinMaxEnabled = true
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
//        // incognito mode in Y axis
//        axisRight.setDrawGridLines(true)
//        axisRight.setDrawLabels(false)
//        axisRight.isEnabled = false
//
//        // labels settings
//        axisLeft.setDrawLabels(false)
//        axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
//        axisLeft.isEnabled = true
//        axisLeft.setDrawGridLines(true)
//        axisLeft.isGranularityEnabled = true
//        axisLeft.granularity = 1f
//        axisLeft.setDrawGridLinesBehindData(false)
//        axisLeft.gridColor = holeDarkBlue
//        axisLeft.setDrawAxisLine(false)
//
//        axisLeft.axisMinimum = -1f
//        axisLeft.axisMaximum = 10f
//
//        // incognito mode for X axis
//        xAxis.setDrawLabels(false)
//        xAxis.labelRotationAngle = 0f
//        xAxis.isEnabled = false
//        xAxis.setDrawGridLines(false)
//        xAxis.setCenterAxisLabels(true)
//        xAxis.setDrawAxisLine(false)
//        xAxis.setDrawGridLines(false)
//        xAxis.position = XAxis.XAxisPosition.TOP_INSIDE
//
//        // incognito mode for char obj
//        legend.isEnabled = false
//        description.isEnabled = false
//
//        // touching do not help at all here on small charts
//        setTouchEnabled(false)
//
//        // do not turn on zoom on such small graph
//        setPinchZoom(false)
//
//        animateX(1800, Easing.EaseInExpo)
//    }

//    fun default() {
//        val vl = defaultDataSettings(LineDataSet(ArrayList(), "Data"))
//        data = LineData(vl)
//
//        defaultAxisSettings()
//        // fill with empty data
//        startTimer()
//    }
}
