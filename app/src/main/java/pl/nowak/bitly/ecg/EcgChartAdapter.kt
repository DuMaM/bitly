package pl.nowak.bitly

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import pl.nowak.bitly.databinding.EcgChartItemBinding
import pl.nowak.bitly.ecg.EcgChart
import pl.nowak.bitly.ecg.EcgChartData
import pl.nowak.bitly.ecg.EcgChartDiffCallback
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class EcgChartListAdapter : ListAdapter<EcgChartData, EcgChartListAdapter.EcgChartViewHolder>(EcgChartDiffCallback()) {

    override fun onBindViewHolder(holder: EcgChartViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EcgChartViewHolder {
        return EcgChartViewHolder.from(parent)
    }

    class EcgChartViewHolder private constructor(private val binding: EcgChartItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val ecgLabel: TextView = binding.ecgLineLabel
        private val ecgChart: EcgChart = binding.ecgChart

        fun bind(item: EcgChartData) {
            val lineSet = ecgChart.defaultDataSettings(LineDataSet(item.lineDataRestricted.toList(), item.label))
            ecgChart.defaultAxisSettings()
            ecgLabel.text = item.label
            ecgChart.data = LineData(lineSet)
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
            fun from(parent: ViewGroup): EcgChartViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = EcgChartItemBinding.inflate(layoutInflater, parent, false)
                return EcgChartViewHolder(binding)
            }
        }
    }
}
