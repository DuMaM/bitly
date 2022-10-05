package pl.nowak.bitly

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.*


class ViewSmallChart : LineChart {
    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet, defStyle: Int) : super(ctx, attrs, defStyle)

    private var numberOfData: Int = 0
    val holoBlue = Color.rgb(51, 181, 229)
    val holeDarkBlue = Color.rgb(103, 134, 147)

    fun moveVisibleWindow(min: Float, max: Float) {
        val x: XAxis = xAxis
        if (min > 0) {
            x.axisMinimum = min
        }
        x.axisMaximum = max
    }

    fun updateData(input: Float, refresh: Boolean = true) {
        val min: Float = (numberOfData - 15).toFloat()
        val max: Float = (numberOfData + 5).toFloat()
        moveVisibleWindow(min, max)

        data.addEntry(Entry(numberOfData.toFloat(), input), 0)

        if (refresh) {
            data.notifyDataChanged()
            notifyDataSetChanged()
            invalidate()
        }
    }

    // used for drawing the line chart
    // when no data is not sent
    private val mTimer: Timer = Timer()

    private fun startTimer() {
        mTimer.schedule(object : TimerTask() {
            override fun run() {
                if (data.entryCount <= numberOfData) {
                    numberOfData = data.entryCount + 1
                    updateData(0f)
                }

                // update current state
                numberOfData = data.entryCount
            }
        }, 0, 5000)
    }

    private fun cancelTimer() {
        mTimer.cancel()
    }

    fun defaultDataSettings(vl: LineDataSet): LineDataSet {
        // draw only space without line and dots
        vl.setDrawFilled(true)
        vl.setDrawValues(false)
        vl.setDrawCircleHole(false)
        vl.setDrawCircles(false)
        vl.lineWidth = 0f

        vl.fillColor = holoBlue
        vl.fillAlpha = holoBlue
        vl.color = holoBlue
        return vl
    }

    fun default() {
        val vl = defaultDataSettings(LineDataSet(ArrayList(), "Data"))
        data = LineData(vl)

        // incognito mode in Y axis
        axisRight.setDrawGridLines(true)
        axisRight.setDrawLabels(false)
        axisRight.isEnabled = false

        axisLeft.setDrawGridLines(true)
        // labels settings
        axisLeft.setDrawLabels(false)
        axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        axisLeft.isEnabled = true
        axisLeft.setDrawGridLines(true)
        axisLeft.isGranularityEnabled = true
        axisLeft.granularity = 1f
        axisLeft.setDrawGridLinesBehindData(false)
        axisLeft.gridColor = holeDarkBlue
        axisLeft.setDrawAxisLine(false)

        axisLeft.axisMinimum = -1f
        axisLeft.axisMaximum = 10f

        // incognito mode for X axis
        xAxis.setDrawLabels(false)
        xAxis.labelRotationAngle = 0f
        xAxis.isEnabled = false
        xAxis.setDrawGridLines(false)
        xAxis.setCenterAxisLabels(true)
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.TOP_INSIDE

        // incognito mode for char obj
        legend.isEnabled = false
        description.isEnabled = false

        // touching do not help at all here on small charts
        setTouchEnabled(false)

        // do not turn on zoom on such small graph
        setPinchZoom(false)

        animateX(1800, Easing.EaseInExpo)

        // fill with empty data
        startTimer()
    }
}
