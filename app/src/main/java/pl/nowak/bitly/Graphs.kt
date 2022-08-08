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


class SmallChart : LineChart {
    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet, defStyle: Int) : super(ctx, attrs, defStyle)

    private var numberOfData: Int = 0

    fun moveVisibleWindow(min: Float, max: Float) {
        val x: XAxis = xAxis
        if (min > 0) {
            x.axisMinimum = min
        }
        x.axisMaximum = max
    }

    // used for drawing the line chart
    // when no data is not sent
    private val mTimer: Timer = Timer()

    private fun startTimer() {
        mTimer.schedule(object : TimerTask() {
            override fun run() {
                if (data.entryCount <= numberOfData) {
                    numberOfData = data.entryCount + 1

                    val min: Float = (numberOfData - 15).toFloat()
                    val max: Float = (numberOfData + 5).toFloat()
                    moveVisibleWindow(min, max)

                    data.addEntry(Entry(numberOfData.toFloat(), 0f), 0)
                    data.notifyDataChanged()
                    notifyDataSetChanged()
                    invalidate()
                }
            }
        }, 0, 1000)
    }

    private fun cancelTimer() {
        mTimer.cancel()
    }

    fun default() {
        val vl = LineDataSet(ArrayList(), "Data")
        val holoBlue = Color.rgb(51, 181, 229)

        // draw only space without line and dots
        vl.setDrawFilled(true)
        vl.setDrawValues(false)
        vl.setDrawCircleHole(false)
        vl.setDrawCircles(false)
        vl.lineWidth = 0f

        vl.fillColor = holoBlue
        vl.fillAlpha = holoBlue
        vl.color = holoBlue
        data = LineData(vl)

        // incognito mode in Y axis
        axisRight.setDrawGridLines(true)
        axisRight.setDrawLabels(false)
        axisRight.isEnabled = false

        axisLeft.setDrawGridLines(true)
        axisLeft.setDrawLabels(true)
        axisLeft.isEnabled = false
        axisLeft.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        axisLeft.setDrawGridLines(true)
        axisLeft.isGranularityEnabled = true
        axisLeft.axisMinimum = -1f
        axisLeft.axisMaximum = 1f


        // incognito mode for X axis
        xAxis.setDrawLabels(false)
        xAxis.labelRotationAngle = 0f
        xAxis.isEnabled = false

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
