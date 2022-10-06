package pl.nowak.bitly

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class AdapterChartList : RecyclerView.Adapter<AdapterChartList.ViewHolderSmallChart>() {

    class EcgChartView(_label: String) {
        var chart: LineDataSet = LineDataSet(ArrayList(), "Data")
        var pos: Int = -1
        val label = _label
    }

    val charts = listOf<EcgChartView>(
        EcgChartView("Lead V1"),
        EcgChartView("Lead V2"),
        EcgChartView("Lead V3"),
        EcgChartView("Lead V4"),
        EcgChartView("Lead V5"),
        EcgChartView("Lead V6"),
        EcgChartView("Lead I"),
        EcgChartView("Lead II"),
        EcgChartView("Lead III"),
        EcgChartView("Lead aVR"),
        EcgChartView("Lead aVL"),
        EcgChartView("Lead aVF")
    )

    override fun getItemCount() = charts.size

    override fun onBindViewHolder(holder: ViewHolderSmallChart, position: Int) {
        val item = charts[position]
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderSmallChart {
        return ViewHolderSmallChart.from(parent)
    }

    class ViewHolderSmallChart private constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val ecgLabel: TextView = itemView.findViewById(R.id.ecgLineLabel)
        val ecgChart: ViewSmallChart = itemView.findViewById(R.id.ecgChart)

        init {
            ecgChart.default()
        }

        fun bind(item: EcgChartView) {
            ecgLabel.text = item.label
        }

        private fun setData(count: Int, range: Double): BarData {
            // now in hours
            val now: Long = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis())
            val values: ArrayList<BarEntry> = ArrayList()

            // count = hours
            val to = (now + count).toFloat()

            // increment by 1 hour
            var x = now.toFloat()
            while (x < to) {
                val y: Double = Random.nextDouble(range, 50.0)
                values.add(BarEntry(x, y.toFloat())) // add one entry per hour
                x++
            }

            // create a dataset and give it a type
            val set1 = BarDataSet(values, "DataSet 1")
            set1.axisDependency = YAxis.AxisDependency.LEFT
            set1.color = ColorTemplate.getHoloBlue()
            set1.valueTextColor = ColorTemplate.getHoloBlue()
            set1.setDrawValues(false)
            set1.highLightColor = Color.rgb(244, 117, 117)

            // create a data object with the data sets
            val data = BarData(set1)
            data.setValueTextColor(Color.WHITE)
            data.setValueTextSize(9f)

            // set data
            return data
        }
        
        companion object {
            fun from(parent: ViewGroup): ViewHolderSmallChart {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.ecg_chart_item, parent, false)
                return ViewHolderSmallChart(view)
            }
        }
    }
}